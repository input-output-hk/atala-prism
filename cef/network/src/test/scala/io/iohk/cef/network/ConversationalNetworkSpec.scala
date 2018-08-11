package io.iohk.cef.network

import java.net.InetSocketAddress
import java.nio.ByteBuffer

import io.iohk.cef.network.NodeId.nodeIdBytes
import io.iohk.cef.network.discovery.NetworkDiscovery
import io.iohk.cef.network.encoding.Codec
import io.iohk.cef.network.encoding.nio.NioCodecs
import io.iohk.cef.network.transport.tcp.NetUtils.{aRandomAddress, randomBytes}
import io.iohk.cef.network.transport.tcp.TcpTransportConfiguration
import org.scalatest.FlatSpec
import org.scalatest.concurrent.Eventually._
import org.scalatest.mockito.MockitoSugar
import org.mockito.Mockito._

class ConversationalNetworkSpec extends FlatSpec with MockitoSugar {

  case class NetworkFixture(nodeId: NodeId,
                            peerInfo: PeerInfo,
                            networkDiscovery: NetworkDiscovery,
                            messageHandler: (NodeId, String) => Unit,
                            network: ConversationalNetwork[String])

  private def randomNetworkFixture(): NetworkFixture = {
    val tcpAddress: InetSocketAddress = aRandomAddress()
    val configuration = ConversationalNetworkConfiguration(Some(TcpTransportConfiguration(tcpAddress)))
    val codec: Codec[String, ByteBuffer] = new Codec(NioCodecs.stringEncoder, NioCodecs.stringDecoder)

    val nodeId = NodeId(randomBytes(nodeIdBytes))

    val networkDiscovery: NetworkDiscovery = mock[NetworkDiscovery]

    val messageHandler: (NodeId, String) => Unit = mock[(NodeId, String) => Unit]

    val peerInfo = PeerInfo(nodeId, configuration)

    val network = new ConversationalNetwork[String](peerInfo,
      messageHandler,
      codec,
      networkDiscovery)

    NetworkFixture(nodeId, peerInfo, networkDiscovery, messageHandler, network)
  }

  behavior of "ConversationalNetwork"

  it should "send a message to a peer that has a common transport" in {
    val alice: NetworkFixture = randomNetworkFixture()
    val bob: NetworkFixture = randomNetworkFixture()

    when(alice.networkDiscovery.peer(bob.nodeId)).thenReturn(Option(bob.peerInfo))

    alice.network.sendMessage(bob.nodeId, "Hi, Bob!")

    eventually {
      verify(bob.messageHandler).apply(alice.nodeId, "Hi, Bob!")
    }
  }

  it should "not send a message to a peer that does not have a common transport"

  it should "send a message to a peer using the peer's NATed address" in pending

  it should "not send a message to an invalid address" in pending

  it should "receive a message from a peer" in pending
}
