package io.iohk.cef.network

import java.net.{Inet6Address, InetAddress, InetSocketAddress, URI}

import akka.util.ByteString
import io.iohk.cef.encoding.rlp.{RLPEncDec, RLPEncodeable, RLPException, RLPList}
import javax.xml.bind.DatatypeConverter
import org.bouncycastle.util.encoders.Hex

import scala.util.Try

case class NodeInfo(id: ByteString,
                    discoveryAddress: InetSocketAddress,
                    serverAddress: InetSocketAddress,
                    capabilities: Capabilities) {

  def idHex: String = Hex.toHexString(id.toArray)

  def getServerUri: URI = {
    val host = getHostName(serverAddress.getAddress)
    new URI(s"enode://${Hex.toHexString(id.toArray[Byte])}@$host:${serverAddress.getPort}?capabilities=${capabilities.byte.toHexString}")
  }

  /**
    * Given an address, returns the corresponding host name for the URI.
    * All IPv6 addresses are enclosed in square brackets.
    *
    * @param address, whose host name will be obtained
    * @return host name associated with the address
    */
  def getHostName(address: InetAddress): String = {
    val hostName = address.getHostAddress
    address match {
      case _: Inet6Address => s"[$hostName]"
      case _ => hostName
    }
  }
}

object NodeInfo {

  implicit def nodeRlpEncDec(implicit
                             byteStrEncDec: RLPEncDec[ByteString],
                             capEncDec: RLPEncDec[Capabilities],
                             inetSocketAddrEncDec: RLPEncDec[InetSocketAddress]) = new RLPEncDec[NodeInfo] {
    override def encode(obj: NodeInfo): RLPEncodeable =
      RLPList(byteStrEncDec.encode(obj.id),
        inetSocketAddrEncDec.encode(obj.discoveryAddress),
        inetSocketAddrEncDec.encode(obj.serverAddress),
        capEncDec.encode(obj.capabilities))

    override def decode(rlp: RLPEncodeable): NodeInfo = rlp match {
      case RLPList(id, discoveryAddr, serverAddr, cap) =>
        NodeInfo(byteStrEncDec.decode(id),
          inetSocketAddrEncDec.decode(discoveryAddr),
          inetSocketAddrEncDec.decode(serverAddr),
          capEncDec.decode(cap))
      case _ => throw new RLPException("src is not a Node")
    }
  }

  def fromUri(p2pUri: URI, discoveryUri: URI, capabilitiesHex: String): Try[NodeInfo] = Try {
    val nodeId = ByteString(Hex.decode(p2pUri.getUserInfo))
    val p2pAddress = InetAddress.getByName(p2pUri.getHost)
    val udpAddress = InetAddress.getByName(discoveryUri.getHost)
    val p2pTcpPort = p2pUri.getPort
    val udpPort = discoveryUri.getPort
    val capabilities = DatatypeConverter.parseHexBinary(capabilitiesHex)(0)

    NodeInfo(nodeId,
      new InetSocketAddress(udpAddress, udpPort),
      new InetSocketAddress(p2pAddress, p2pTcpPort),
      Capabilities(capabilities))
  }
}
