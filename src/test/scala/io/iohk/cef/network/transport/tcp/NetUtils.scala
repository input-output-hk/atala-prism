package io.iohk.cef.network.transport.tcp

import java.io.OutputStream
import java.net.{InetSocketAddress, ServerSocket, Socket}
import java.nio.ByteBuffer

import io.iohk.cef.network.{ConversationalNetworkConfiguration, NodeId, PeerInfo}
import io.iohk.cef.network.NodeId.nodeIdBytes
import io.iohk.cef.network.discovery.NetworkDiscovery
import io.iohk.cef.network.transport.{FrameHeader, Transports}
import org.mockito.Mockito.when
import org.scalacheck.Gen
import org.scalacheck.Arbitrary._
import org.scalatest.mockito.MockitoSugar.mock

import scala.collection.mutable
import scala.util.Random

object NetUtils {
  def writeTo(address: InetSocketAddress, bytes: Array[Byte]): Unit = {
    val socket = new Socket(address.getHostName, address.getPort)
    val out: OutputStream = socket.getOutputStream
    try {
      out.write(bytes)
    } finally {
      out.close()
    }
  }

  def aRandomNodeId(): NodeId =
    NodeId(randomBytes(nodeIdBytes))

  def aRandomAddress(): InetSocketAddress = {
    val s = new ServerSocket(0)
    try {
      new InetSocketAddress("localhost", s.getLocalPort)
    } finally {
      s.close()
    }
  }

  def discardMessages[Addr, T](remoteAddress: Addr, message: T): Unit = ()

  def logMessages[T](messages: mutable.ListBuffer[T])(remoteAddress: InetSocketAddress, message: T): Unit =
    messages += message

  def isListening(address: InetSocketAddress): Boolean = {
    try {
      new Socket(address.getHostName, address.getPort).close()
      true
    } catch {
      case e: Exception =>
        false
    }
  }

  def toArray(b: ByteBuffer): Array[Byte] = {
    val a = new Array[Byte](b.remaining())
    b.get(a)
    a
  }

  def randomBytes(n: Int): Array[Byte] = {
    val a = new Array[Byte](n)
    Random.nextBytes(a)
    a
  }

  def concatenate(buffs: Seq[ByteBuffer]): ByteBuffer = {
    val allocSize = buffs.foldLeft(0)((acc, nextBuff) => acc + nextBuff.capacity())

    val b0 = ByteBuffer.allocate(allocSize)

    buffs.foldLeft(b0)((accBuff, nextBuff) => accBuff.put(nextBuff)).flip().asInstanceOf[ByteBuffer]
  }

  def forwardPort(srcPort: Int, dst: InetSocketAddress): PortForward =
    new PortForward(srcPort, dst)

  object NetUtilsGen {
    val genNodeId: Gen[NodeId] = Gen.listOfN(NodeId.nodeIdBytes, arbitrary[Byte]).map(NodeId(_))

    val genPeerInfo: Gen[PeerInfo] = for {
      nodeId <- genNodeId
    } yield {
      val address = aRandomAddress()
      val messageTtl = FrameHeader.defaultTtl
      PeerInfo(nodeId, ConversationalNetworkConfiguration(Some(TcpTransportConfiguration(address)), messageTtl))
    }
  }

  case class NetworkFixture(
      nodeId: NodeId,
      peerInfo: PeerInfo,
      networkDiscovery: NetworkDiscovery,
      transports: Transports)

  def randomNetworkFixture(messageTtl: Int = FrameHeader.defaultTtl): NetworkFixture = {

    val tcpAddress: InetSocketAddress = aRandomAddress()
    val configuration = ConversationalNetworkConfiguration(Some(TcpTransportConfiguration(tcpAddress)), messageTtl)

    val nodeId = NodeId(randomBytes(NodeId.nodeIdBytes))

    val networkDiscovery: NetworkDiscovery = mock[NetworkDiscovery]

    val peerInfo = PeerInfo(nodeId, configuration)

    NetworkFixture(nodeId, peerInfo, networkDiscovery, new Transports(peerInfo))
  }

  def nodesArePeers(node1: NetworkFixture, node2: NetworkFixture): Unit = {
    when(node1.networkDiscovery.nearestPeerTo(node2.nodeId)).thenReturn(Some(node2.peerInfo))
    when(node2.networkDiscovery.nearestPeerTo(node1.nodeId)).thenReturn(Some(node1.peerInfo))
  }
}
