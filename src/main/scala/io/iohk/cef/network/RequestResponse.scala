package io.iohk.cef.network

import java.util.UUID
import java.util.UUID.randomUUID
import java.util.concurrent.ConcurrentHashMap

import io.iohk.cef.network.discovery.NetworkDiscovery
import io.iohk.cef.codecs.nio.{NioDecoder, NioEncoder}
import io.iohk.cef.codecs.nio._
import io.iohk.cef.network.transport.Transports

import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.{Failure, Success}
import scala.reflect.runtime.universe.TypeTag

// FIXME: this will need a way of knowing if the other side
// actually cares about the request (without reinventing HTTP)
class RequestResponse[Request: NioEncoder: NioDecoder, Response: NioEncoder: NioDecoder](
    networkDiscovery: NetworkDiscovery,
    transports: Transports)(implicit ec: ExecutionContext) {

  private implicit val requestTypeTag: TypeTag[Request] = NioEncoder[Request].typeTag
  private implicit val responseTypeTag: TypeTag[Response] = NioEncoder[Response].typeTag

  private val correlationMap = new ConcurrentHashMap[UUID, Promise[Response]]().asScala

  private val requestChannel: ConversationalNetwork[Correlated[Request]] =
    new ConversationalNetwork[Correlated[Request]](networkDiscovery, transports)

  private val responseChannel: ConversationalNetwork[Correlated[Response]] =
    new ConversationalNetwork[Correlated[Response]](networkDiscovery, transports)

  responseChannel.messageStream.foreach(processResponse)

  private def processResponse(response: Correlated[Response]): Unit = {
    correlationMap
      .get(response.correlationId)
      .foreach(promisedResponse => {
        promisedResponse.success(response.content)
        correlationMap.remove(response.correlationId)
      })
  }

  def sendRequest(nodeId: NodeId, request: Request): Future[Response] = {
    val correlationId = randomUUID()
    val responsePromise = Promise[Response]()
    correlationMap.put(correlationId, responsePromise)
    requestChannel.sendMessage(nodeId, Correlated(correlationId, transports.peerInfo.nodeId, request))
    responsePromise.future
  }

  def handleRequest(f: Request => Response): Unit = {
    requestChannel.messageStream.foreach(correlatedRequest => { // FIXME will need to handle exceptions
      val response = Correlated(
        correlationId = correlatedRequest.correlationId,
        from = transports.peerInfo.nodeId,
        content = f(correlatedRequest.content))
      responseChannel.sendMessage(correlatedRequest.from, response)
    })
  }

  def handleFutureRequest(f: Request => Future[Response]): Unit = {
    requestChannel.messageStream.foreach(correlatedRequest => {
      f(correlatedRequest.content).onComplete {
        case Success(content) =>
          val response = Correlated(
            correlationId = correlatedRequest.correlationId,
            from = transports.peerInfo.nodeId,
            content = content
          )
          responseChannel.sendMessage(correlatedRequest.from, response)
        case Failure(exception) => ??? // FIXME
      }
    })
  }
}

case class Correlated[T](correlationId: UUID, from: NodeId, content: T)
object Correlated {
  implicit def CorrelatedEncDec[T: NioEncDec]: NioEncDec[Correlated[T]] = {
    import io.iohk.cef.codecs.nio.auto._
    implicit val ttt: TypeTag[T] = NioEncDec[T].typeTag
    val e: NioEncoder[Correlated[T]] = genericEncoder
    val d: NioDecoder[Correlated[T]] = genericDecoder
    NioEncDec(e, d)
  }
}
