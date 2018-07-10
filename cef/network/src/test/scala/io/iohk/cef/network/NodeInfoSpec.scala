package io.iohk.cef.network

import java.net.{Inet6Address, InetSocketAddress, URI}

import akka.util.ByteString
import org.bouncycastle.util.encoders.Hex
import org.scalatest.FlatSpec
import org.scalatest.Matchers._

class NodeInfoSpec extends FlatSpec {

  val loopbackAddress: Array[Byte] = Array(
    0x00.toByte, 0x00.toByte, 0x00.toByte, 0x00.toByte,
    0x00.toByte, 0x00.toByte, 0x00.toByte, 0x00.toByte,
    0x00.toByte, 0x00.toByte, 0x00.toByte, 0x00.toByte,
    0x00.toByte, 0x00.toByte, 0x00.toByte, 0x01.toByte)

  val ip4Address = new InetSocketAddress("localhost", 3000)
  val ip6Address = new InetSocketAddress(Inet6Address.getByAddress("localhost", loopbackAddress, 0), 3000)

  val id = "761d11916c0baf6632134cf5a55d3bdf821ee2e9f8b76ee4b7f8c7246d345fcf15099965c5f2d4adfaafbb9721202ee7b71eb3ccf1d96a1489f63506b498f1cb"

  "NodeInfo" should "correctly create a node uri for IPv4" in {
    val nodeInfo = NodeInfo(ByteString(Hex.decode(id)), ip4Address, ip4Address, Capabilities(1))

    nodeInfo.getServerUri shouldBe new URI("enode://761d11916c0baf6632134cf5a55d3bdf821ee2e9f8b76ee4b7f8c7246d345fcf15099965c5f2d4adfaafbb9721202ee7b71eb3ccf1d96a1489f63506b498f1cb@127.0.0.1:3000?capabilities=1")
  }

  it should "correctly create a node uri for IPv6" in {
    val nodeInfo = NodeInfo(ByteString(Hex.decode(id)), ip6Address, ip6Address, Capabilities(1))
    nodeInfo.getServerUri shouldBe new URI("enode://761d11916c0baf6632134cf5a55d3bdf821ee2e9f8b76ee4b7f8c7246d345fcf15099965c5f2d4adfaafbb9721202ee7b71eb3ccf1d96a1489f63506b498f1cb@[0:0:0:0:0:0:0:1%0]:3000?capabilities=1")
  }
}
