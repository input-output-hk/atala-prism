package io.iohk.cef.network

import java.net.InetSocketAddress

import io.iohk.cef.network.discovery.NetworkDiscovery
import io.iohk.cef.network.transport.{Frame, FrameHeader, NetworkTransport, Transports}
import io.iohk.cef.network.transport.tcp.TcpTransportConfiguration
import org.scalatest.FlatSpec
import org.scalatest.mockito.MockitoSugar._
import org.mockito.Mockito.{verify, when}
import org.mockito.ArgumentMatchers.any
import io.iohk.cef.network.encoding.nio._
import io.iohk.cef.network.transport.tcp.NetUtils
import io.iohk.cef.network.transport.tcp.NetUtils.NetUtilsGen.genPeerInfo
import org.scalacheck.Gen._
import org.scalatest.prop.GeneratorDrivenPropertyChecks._

class DisseminationalNetworkSpec extends FlatSpec {

  behavior of "Disseminational Network"

  it should "disseminate a message to its peers" in {
    forAll(listOfN(2, genPeerInfo)) { peers =>
      val message = "Hello, world!"
      val discovery = mock[NetworkDiscovery]
      val transports = mock[Transports]
      val nodeId = NetUtils.aRandomNodeId()
      val tcpTransport = mock[NetworkTransport[InetSocketAddress, Frame[String]]]

      when(transports.peerInfo).thenReturn(
        PeerInfo(
          nodeId,
          ConversationalNetworkConfiguration(Some(TcpTransportConfiguration(NetUtils.aRandomAddress())))))
      when(transports.tcp[Frame[String]](any())).thenReturn(Some(tcpTransport))
      peers.foreach(peer => when(discovery.nearestPeerTo(peer.nodeId)).thenReturn(Some(peer)))
      when(discovery.nearestNPeersTo(nodeId, Int.MaxValue)).thenReturn(peers)

      val network = new DisseminationalNetwork[String](discovery, transports)
      network.disseminateMessage(message)

      peers.foreach(peer => {
        val expectedMessageFrame = Frame(FrameHeader(nodeId, peer.nodeId), message)
        verify(tcpTransport)
          .sendMessage(peer.configuration.tcpTransportConfiguration.get.bindAddress, expectedMessageFrame)
      })
    }
  }

}
