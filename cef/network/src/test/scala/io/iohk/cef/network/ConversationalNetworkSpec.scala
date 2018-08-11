package io.iohk.cef.network

import java.net.InetSocketAddress
import java.nio.ByteBuffer

import io.iohk.cef.network.discovery.NetworkDiscovery
import io.iohk.cef.network.encoding.Codec
import io.iohk.cef.network.transport.tcp.NetUtils.aRandomAddress
import io.iohk.cef.network.transport.tcp.TcpTransportConfiguration
import org.scalamock.scalatest.MockFactory
import org.scalatest.FlatSpec

class ConversationalNetworkSpec extends FlatSpec with MockFactory {

  trait GivenTestFixture {

    val networkDiscovery: NetworkDiscovery = mock[NetworkDiscovery]

    val tcpAddress: InetSocketAddress = aRandomAddress()

    val configuration = ConversationalNetworkConfiguration(Some(TcpTransportConfiguration(tcpAddress)))

    val peerInfo = PeerInfo(NodeId("Alice"), configuration)

    val messageHandler: (NodeId, String) => Unit = mockFunction[NodeId, String, Unit]

    val codec: Codec[String, ByteBuffer] = ???

    val network = new ConversationalNetwork[String](peerInfo, messageHandler, codec, networkDiscovery)
  }

  behavior of "ConversationalNetwork"

  it should "not send a message to a peer that does not have a common transport"

  it should "send a message to a peer that does have a common transport" in pending

  it should "send a message to a peer using the peer's NATed address" in pending

  it should "not send a message to an invalid address" in pending

  it should "receive a message from a peer" in pending
}
