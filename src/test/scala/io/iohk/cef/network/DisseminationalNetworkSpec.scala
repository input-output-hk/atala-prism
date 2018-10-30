package io.iohk.cef.network

import java.net.InetSocketAddress

import io.iohk.cef.network.discovery.NetworkDiscovery
import io.iohk.cef.codecs.nio._
import io.iohk.cef.network.transport.tcp.NetUtils.{aRandomAddress, aRandomNodeId}
import io.iohk.cef.network.transport.tcp.TcpTransportConfiguration
import io.iohk.cef.network.transport.{Frame, FrameHeader, NetworkTransport, Transports}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{verify, when}
import org.scalatest.FlatSpec
import org.scalatest.mockito.MockitoSugar._

class DisseminationalNetworkSpec extends FlatSpec {

  behavior of "Disseminational Network"

  val nodeId = aRandomNodeId()
  val address = aRandomAddress()
  val transports = mock[Transports]
  val message = "Hello, world!"

  val peer1 =
    PeerInfo(aRandomNodeId(), NetworkConfiguration(Some(TcpTransportConfiguration(aRandomAddress()))))
  val peer2 =
    PeerInfo(aRandomNodeId(), NetworkConfiguration(Some(TcpTransportConfiguration(aRandomAddress()))))

  it should "disseminate a message to its peers" in {
    val peers = List(peer1, peer2)
    val discovery = mock[NetworkDiscovery]
    val tcpTransport = mock[NetworkTransport[InetSocketAddress, Frame[String]]]
    //    println(peers)
    peers.foreach(peer => when(discovery.nearestPeerTo(peer.nodeId)).thenReturn(Some(peer)))
    when(discovery.nearestNPeersTo(nodeId, Int.MaxValue)).thenReturn(peers)
    when(transports.peerInfo)
      .thenReturn(PeerInfo(nodeId, NetworkConfiguration(Some(TcpTransportConfiguration(address)))))
    when(transports.tcp[Frame[String]](any())).thenReturn(Some(tcpTransport))

    val network = new DisseminationalNetwork[String](discovery, transports)

    network.disseminateMessage(message)

    peers.foreach(peer => {
      val expectedMessageFrame = Frame(FrameHeader(nodeId, peer.nodeId), message)
      verify(tcpTransport)
        .sendMessage(peer.configuration.tcpTransportConfiguration.get.bindAddress, expectedMessageFrame)
    })
  }

}
