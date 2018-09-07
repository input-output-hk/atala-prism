package io.iohk.cef.network
import java.net.InetSocketAddress

import io.iohk.cef.network.discovery.NetworkDiscovery
import io.iohk.cef.network.encoding.nio._
import io.iohk.cef.network.transport.tcp.NetUtils.{aRandomAddress, randomBytes}
import io.iohk.cef.network.transport.tcp.TcpTransportConfiguration
import io.iohk.cef.network.transport.{FrameHeader, Transports}
import org.mockito.Mockito.when
import org.scalatest.FlatSpec
import org.scalatest.Matchers._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.concurrent.ScalaFutures.convertScalaFuture
import org.scalatest.mockito.MockitoSugar._

import scala.concurrent.duration._

class RequestResponseSpec extends FlatSpec {

  private implicit val patienceConfig = ScalaFutures.PatienceConfig(timeout = 1 seconds, interval = 50 millis)

  behavior of "RequestResponse"

  case class Request(content: String)
  case class Response(content: String)

  it should "send a request and receive a response" in {

    val alicesRequest = Request("Hi! I'm Alice. What's your name?")
    val bobsResponse = Response("Hi, Alice!. I'm Bob.")

    val alicesNetwork = randomNetworkFixture()
    val bobsNetwork = randomNetworkFixture()

    when(alicesNetwork.networkDiscovery.nearestPeerTo(bobsNetwork.nodeId)).thenReturn(Some(bobsNetwork.peerInfo))
    when(bobsNetwork.networkDiscovery.nearestPeerTo(alicesNetwork.nodeId)).thenReturn(Some(alicesNetwork.peerInfo))

    val alicesSide = new RequestResponse[Request, Response](alicesNetwork.networkDiscovery, alicesNetwork.transports)

    val bobsSide = new RequestResponse[Request, Response](bobsNetwork.networkDiscovery, bobsNetwork.transports)

    bobsSide.handleRequest(request => {
      if (request == alicesRequest) {
        bobsResponse
      } else
        fail("Received an invalid request")
    })


    val response: Response = alicesSide.sendRequest(bobsNetwork.nodeId, alicesRequest).futureValue

    response shouldBe bobsResponse
  }

  private case class NetworkFixture(
      nodeId: NodeId,
      peerInfo: PeerInfo,
      networkDiscovery: NetworkDiscovery,
      transports: Transports)

  private def randomNetworkFixture(messageTtl: Int = FrameHeader.defaultTtl): NetworkFixture = {

    val tcpAddress: InetSocketAddress = aRandomAddress()
    val configuration = ConversationalNetworkConfiguration(Some(TcpTransportConfiguration(tcpAddress)), messageTtl)

    val nodeId = NodeId(randomBytes(NodeId.nodeIdBytes))

    val networkDiscovery: NetworkDiscovery = mock[NetworkDiscovery]

    val peerInfo = PeerInfo(nodeId, configuration)

    NetworkFixture(nodeId, peerInfo, networkDiscovery, new Transports(peerInfo))
  }
}
