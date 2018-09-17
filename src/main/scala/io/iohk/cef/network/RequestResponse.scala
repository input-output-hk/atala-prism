package io.iohk.cef.network

import java.util.UUID
import java.util.UUID.randomUUID
import java.util.concurrent.ConcurrentHashMap

import io.iohk.cef.network.discovery.NetworkDiscovery
import io.iohk.cef.network.encoding.nio.{NioDecoder, NioEncoder}
import io.iohk.cef.network.encoding.nio._
import io.iohk.cef.network.transport.Transports

import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.{Failure, Success}

// FIXME: this will need a way of knowing if the other side
// actually cares about the request (without reinventing HTTP)
class RequestResponse[Request: NioEncoder: NioDecoder, Response: NioEncoder: NioDecoder](
    networkDiscovery: NetworkDiscovery,
    transports: Transports)(implicit ec: ExecutionContext) {

  case class CorrelatedRequest(correlationId: UUID, from: NodeId, content: Request)

  case class CorrelatedResponse(correlationId: UUID, from: NodeId, content: Response)

  private val correlationMap = new ConcurrentHashMap[UUID, Promise[Response]]().asScala

  private val requestChannel: ConversationalNetwork[CorrelatedRequest] =
    new ConversationalNetwork[CorrelatedRequest](networkDiscovery, transports)

  private val responseChannel: ConversationalNetwork[CorrelatedResponse] =
    new ConversationalNetwork[CorrelatedResponse](networkDiscovery, transports)

  responseChannel.messageStream.foreach(processResponse)

  private def processResponse(response: CorrelatedResponse): Unit = {
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
    requestChannel.sendMessage(nodeId, CorrelatedRequest(correlationId, transports.peerInfo.nodeId, request))
    responsePromise.future
  }

  def handleRequest(f: Request => Response): Unit = {
    requestChannel.messageStream.foreach(correlatedRequest => { // FIXME will need to handle exceptions
      val response = CorrelatedResponse(
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
          val response = CorrelatedResponse(
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
