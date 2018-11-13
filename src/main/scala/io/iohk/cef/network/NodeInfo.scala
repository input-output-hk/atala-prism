package io.iohk.cef.network

import java.net.{Inet6Address, InetAddress, InetSocketAddress, URI}

import akka.util.ByteString
import io.iohk.cef.network.encoding.rlp.{RLPEncDec, RLPEncodeable, RLPException, RLPList}
import io.iohk.cef.network.transport.FrameHeader
import io.iohk.cef.network.transport.tcp.TcpTransportConfig
import javax.xml.bind.DatatypeConverter
import io.iohk.cef.utils.HexStringCodec._

import scala.util.Try

/**
  * TODO this is currently hardcoded only to work with rlpx on a tcp/ip network.
  * FIXME replace NodeInfo with PeerInfo and NodeId
  */
case class NodeInfo(
    id: ByteString,
    discoveryAddress: InetSocketAddress,
    serverAddress: InetSocketAddress,
    capabilities: Capabilities) {

  def getServerUri: URI = {
    val host = getHostName(serverAddress.getAddress)
    new URI(s"enode://${toHexString(id)}@$host:${serverAddress.getPort}?capabilities=${capabilities.byte.toHexString}")
  }

  /**
    * Given an address, returns the corresponding host name for the URI.
    * All IPv6 addresses are enclosed in square brackets.
    *
    * @param address, whose host name will be obtained
    * @return host name associated with the address
    */
  private def getHostName(address: InetAddress): String = {
    val hostName = address.getHostAddress
    address match {
      case _: Inet6Address => s"[$hostName]"
      case _ => hostName
    }
  }

  def toPeerInfo: PeerConfig = {
    val itsNodeId = NodeId(id)
    val itsConfiguration =
      NetworkConfig(Some(TcpTransportConfig(serverAddress)), FrameHeader.defaultTtl)
    PeerConfig(itsNodeId, itsConfiguration)
  }
}

object NodeInfo {

  implicit def nodeRlpEncDec(
      implicit
      byteStrEncDec: RLPEncDec[ByteString],
      capEncDec: RLPEncDec[Capabilities],
      inetSocketAddrEncDec: RLPEncDec[InetSocketAddress]) = new RLPEncDec[NodeInfo] {
    override def encode(obj: NodeInfo): RLPEncodeable =
      RLPList(
        byteStrEncDec.encode(obj.id),
        inetSocketAddrEncDec.encode(obj.discoveryAddress),
        inetSocketAddrEncDec.encode(obj.serverAddress),
        capEncDec.encode(obj.capabilities)
      )

    override def decode(rlp: RLPEncodeable): NodeInfo = rlp match {
      case RLPList(id, discoveryAddr, serverAddr, cap) =>
        NodeInfo(
          byteStrEncDec.decode(id),
          inetSocketAddrEncDec.decode(discoveryAddr),
          inetSocketAddrEncDec.decode(serverAddr),
          capEncDec.decode(cap))
      case _ => throw new RLPException("src is not a Node")
    }
  }

  def fromUri(p2pUri: URI, discoveryUri: URI, capabilitiesHex: String): Try[NodeInfo] = Try {
    val nodeId = fromHexString(p2pUri.getUserInfo)
    val p2pAddress = InetAddress.getByName(p2pUri.getHost)
    val udpAddress = InetAddress.getByName(discoveryUri.getHost)
    val p2pTcpPort = p2pUri.getPort
    val udpPort = discoveryUri.getPort
    val capabilities = DatatypeConverter.parseHexBinary(capabilitiesHex)(0)

    NodeInfo(
      nodeId,
      new InetSocketAddress(udpAddress, udpPort),
      new InetSocketAddress(p2pAddress, p2pTcpPort),
      Capabilities(capabilities))
  }
}
