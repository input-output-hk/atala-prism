package io.iohk.cef.network.transport
import java.net.InetSocketAddress

import io.iohk.cef.network.transport.tcp.NetUtils.aRandomAddress
import io.iohk.cef.network.transport.tcp.TcpTransportConfiguration
import io.iohk.cef.network.{ConversationalNetworkConfiguration, NodeId, PeerInfo}
import io.iohk.cef.network.encoding.nio.NioCodecs.NioStreamCodec

import org.scalatest.FlatSpec
import org.scalatest.Matchers._
import org.scalatest.mockito.MockitoSugar._

class TransportsSpec extends FlatSpec {

  behavior of "Transports"

  it should "say usesTcp = true if tcp is configured" in {
    val peerInfo = PeerInfo(NodeId(List(0.toByte, 0.toByte)),
                            ConversationalNetworkConfiguration(Some(TcpTransportConfiguration(aRandomAddress()))))

    Transports.usesTcp(peerInfo) shouldBe true
  }

  it should "say usesTcp = false if tcp is not configured" in {
    val peerInfo = PeerInfo(NodeId(List(0.toByte, 0.toByte)),
                            ConversationalNetworkConfiguration(None))

    Transports.usesTcp(peerInfo) shouldBe false
  }

  it should "initialize netty if tcp is configured" in {
    val peerInfo = PeerInfo(NodeId(List(0.toByte, 0.toByte)),
                            ConversationalNetworkConfiguration(Some(TcpTransportConfiguration(aRandomAddress()))))

    val transports = new Transports(peerInfo)

    transports.netty() shouldBe defined
  }

  it should "not initialize netty if tcp is not configured" in {
    val peerInfo = PeerInfo(NodeId(List(0.toByte, 0.toByte)), ConversationalNetworkConfiguration(None))

    val transports = new Transports(peerInfo)

    transports.netty() shouldBe None
  }

  it should "not initialize netty twice" in {
    val peerInfo = PeerInfo(NodeId(List(0.toByte, 0.toByte)),
                            ConversationalNetworkConfiguration(Some(TcpTransportConfiguration(aRandomAddress()))))

    val transports = new Transports(peerInfo)

    transports.netty() shouldBe transports.netty()
  }

  it should "return tcp if tcp is configured" in {
    val peerInfo = PeerInfo(NodeId(List(0.toByte, 0.toByte)),
                            ConversationalNetworkConfiguration(Some(TcpTransportConfiguration(aRandomAddress()))))

    val transports = new Transports(peerInfo)

    transports.tcp(mock[(InetSocketAddress, String) => Unit])(mock[NioStreamCodec[String]]) shouldBe defined
  }

  it should "not return tcp if tcp is not configured" in {
    val peerInfo = PeerInfo(NodeId(List(0.toByte, 0.toByte)),
                            ConversationalNetworkConfiguration(None))

    val transports = new Transports(peerInfo)

    transports.tcp(mock[(InetSocketAddress, String) => Unit])(mock[NioStreamCodec[String]]) shouldBe None
  }
}
