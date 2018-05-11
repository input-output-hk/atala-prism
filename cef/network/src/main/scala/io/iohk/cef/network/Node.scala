package io.iohk.cef.network

import java.net.{InetAddress, URI}

import akka.util.ByteString
import io.iohk.cef.encoding.rlp.{RLPEncDec, RLPEncodeable, RLPException, RLPList}
import javax.xml.bind.DatatypeConverter
import org.bouncycastle.util.encoders.Hex

import scala.util.Try

case class Node(id: ByteString, endpoint: Endpoint, capabilities: Capabilities) {

  def toUri: URI = {
    val host = Endpoint.getHostName(endpoint.address)
    new URI(s"enode://${id.utf8String}@$host:${endpoint.tcpPort}?discport=${endpoint.udpPort}&capabilities=${capabilities.byte.toHexString}")
  }
}

object Node {

  implicit def nodeRlpEncDec(implicit
                             byteStrEncDec: RLPEncDec[ByteString],
                             nodeAddrEncDec: RLPEncDec[Endpoint],
                             capEncDec: RLPEncDec[Capabilities]) = new RLPEncDec[Node] {
    override def encode(obj: Node): RLPEncodeable =
      RLPList(byteStrEncDec.encode(obj.id), nodeAddrEncDec.encode(obj.endpoint), capEncDec.encode(obj.capabilities))

    override def decode(rlp: RLPEncodeable): Node = rlp match {
      case RLPList(id, addr, cap) =>
        Node(byteStrEncDec.decode(id), nodeAddrEncDec.decode(addr), capEncDec.decode(cap))
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

    Node(nodeId, Endpoint(address, udpPort.getOrElse(tcpPort), tcpPort), Capabilities(capabilities))
  }
}
