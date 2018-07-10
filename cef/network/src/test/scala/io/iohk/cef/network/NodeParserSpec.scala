package io.iohk.cef.network

import com.typesafe.config.ConfigFactory
import org.scalatest.{FlatSpec, MustMatchers}

class NodeParserSpec extends FlatSpec with MustMatchers {

  behavior of "NodeParser"

  it should "parse a Node" in {
    import collection.JavaConverters._
    val id = "761d11916c0baf6632134cf5a55d3bdf821ee2e9f8b76ee4b7f8c7246d345fcf15099965c5f2d4adfaafbb9721202ee7b71eb3ccf1d96a1489f63506b498f1cb"
    val udpAddress = "udp://127.0.0.1:1000"
    val p2pAddress = s"enode://${id}@127.0.0.2:3000"
    val capabilities = "01"
    val config = ConfigFactory.parseString(
      s"""
         |nodes = [
         |    {
         |      discoveryUri = "$udpAddress"
         |      p2pUri = "$p2pAddress"
         |      capabilities = $capabilities
         |    }
         |  ]
      """.stripMargin).getConfigList("nodes")
    val parsed = NodeParser.parseNodeInfos(config.asScala.toSet)
    parsed.size mustBe 1
    val node = parsed.head
    node.discoveryAddress.getAddress.getAddress.toList mustBe List[Byte](127,0,0,1)
    node.serverAddress.getAddress.getAddress.toList mustBe List[Byte](127,0,0,2)
    node.discoveryAddress.getPort mustBe 1000
    node.serverAddress.getPort mustBe 3000
    node.capabilities.byte mustBe 0x1
  }
}
