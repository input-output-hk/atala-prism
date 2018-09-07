package io.iohk.cef.network
import java.util.UUID
import java.util.UUID.randomUUID
import java.util.concurrent.ConcurrentHashMap

import io.iohk.cef.network.RequestResponse.{CorrelatedRequest, CorrelatedResponse}
import io.iohk.cef.network.discovery.NetworkDiscovery
import io.iohk.cef.network.encoding.nio.{NioDecoder, NioEncoder}
import io.iohk.cef.network.encoding.nio._
import io.iohk.cef.network.transport.Transports

import scala.collection.JavaConverters._
import scala.concurrent.{Future, Promise}

private[network] object RequestResponse {
  case class CorrelatedRequest[Request](correlationId: UUID, content: Request)
  case class CorrelatedResponse[Response](correlationId: UUID, content: Response)
}

// FIXME: this will need a way of knowing if the other side
// actually cares about the request (without reinventing HTTP)
private[network] class RequestResponse[Request: NioEncoder: NioDecoder, Response: NioEncoder: NioDecoder](
    networkDiscovery: NetworkDiscovery,
    transports: Transports) {

  private val correlationMap = new ConcurrentHashMap[UUID, Promise[Response]]().asScala

  private val requestSender: ConversationalNetwork[CorrelatedRequest[Request]] =
    new ConversationalNetwork[CorrelatedRequest[Request]](networkDiscovery, transports)

  private val responseReceiver: ConversationalNetwork[CorrelatedResponse[Response]] =
    new ConversationalNetwork[CorrelatedResponse[Response]](networkDiscovery, transports)

  responseReceiver.messageStream.foreach(processResponse)

  private def processResponse(response: CorrelatedResponse[Response]): Unit = {
    val p = correlationMap.get(response.correlationId)
    p.foreach(promisedResponse => {
      promisedResponse.success(response.content)
      correlationMap.remove(response.correlationId)
    })
  }

  def sendRequest(nodeId: NodeId, request: Request): Future[Response] = {
    val correlationId = randomUUID()
    val responsePromise = Promise[Response]()
    correlationMap.put(correlationId, responsePromise)
    requestSender.sendMessage(nodeId, CorrelatedRequest(correlationId, request))
    responsePromise.future
  }
}
