package io.iohk.cef.network

import java.net.{InetAddress, InetSocketAddress, URI}

import akka.util.ByteString
import io.iohk.cef.encoding.rlp.{RLPEncDec, RLPEncodeable, RLPException, RLPList}
import javax.xml.bind.DatatypeConverter
import org.bouncycastle.util.encoders.Hex

import scala.util.Try

case class Node(id: ByteString,
                discoveryAddress: InetSocketAddress,
                serverAddress: InetSocketAddress,
                capabilities: Capabilities) {

  def idHex: String = Hex.toHexString(id.toArray)

  def toUri: URI = {
    val host = Endpoint.getHostName(serverAddress.getAddress)
    new URI(s"enode://${Hex.toHexString(id.toArray[Byte])}@$host:${serverAddress.getPort}?discport=${discoveryAddress.getPort}&capabilities=${capabilities.byte.toHexString}")
  }
}

object Node {

  implicit def nodeRlpEncDec(implicit
                             byteStrEncDec: RLPEncDec[ByteString],
                             nodeAddrEncDec: RLPEncDec[Endpoint],
                             capEncDec: RLPEncDec[Capabilities],
                             inetSocketAddrEncDec: RLPEncDec[InetSocketAddress]) = new RLPEncDec[Node] {
    override def encode(obj: Node): RLPEncodeable =
      RLPList(byteStrEncDec.encode(obj.id),
        inetSocketAddrEncDec.encode(obj.discoveryAddress),
        inetSocketAddrEncDec.encode(obj.serverAddress),
        capEncDec.encode(obj.capabilities))

    override def decode(rlp: RLPEncodeable): Node = rlp match {
      case RLPList(id, discoveryAddr, serverAddr, cap) =>
        Node(byteStrEncDec.decode(id),
          inetSocketAddrEncDec.decode(discoveryAddr),
          inetSocketAddrEncDec.decode(serverAddr),
          capEncDec.decode(cap))
      case _ => throw new RLPException("src is not a Node")
    }
  }

  def fromUri(p2pUri: URI, discoveryUri: URI, capabilitiesHex: String): Try[Node] = Try {
    val nodeId = ByteString(Hex.decode(p2pUri.getUserInfo))
    val p2pAddress = InetAddress.getByName(p2pUri.getHost)
    val udpAddress = InetAddress.getByName(discoveryUri.getHost)
    val p2pTcpPort = p2pUri.getPort
    val udpPort = discoveryUri.getPort
    val capabilities = DatatypeConverter.parseHexBinary(capabilitiesHex)(0)

    Node(nodeId,
      new InetSocketAddress(udpAddress, udpPort),
      new InetSocketAddress(p2pAddress, p2pTcpPort),
      Capabilities(capabilities))
  }
}
