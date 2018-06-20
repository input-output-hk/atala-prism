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

  def fromUri(uri: URI): Try[Node] = Try {
    val nodeId = ByteString(Hex.decode(uri.getUserInfo))
    val address = InetAddress.getByName(uri.getHost)
    val tcpPort = uri.getPort
    val parsedQuery = Option(uri.getQuery).map(query =>{
      query.split("&").map { assignment =>
        val valuePair = assignment.split("=")
        if (valuePair.size == 2)
          Option((valuePair(0), valuePair(1)))
        else
          None
      }.flatten
    }.toMap)
    val queryMap = parsedQuery.getOrElse(Map())
    val udpPort = queryMap.get("discport").map(_.toInt)
    val capabilitiesHex =
      queryMap.get("capabilities").getOrElse(throw new IllegalArgumentException("Node URI does not have a capabilities value"))
    val capabilities = DatatypeConverter.parseHexBinary(capabilitiesHex)(0)

    Node(nodeId,
      new InetSocketAddress(address, udpPort.getOrElse(tcpPort)),
      new InetSocketAddress(address, tcpPort),
      Capabilities(capabilities))
  }
}
