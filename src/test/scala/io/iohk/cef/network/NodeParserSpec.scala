package io.iohk.cef.network

import com.typesafe.config.{Config, ConfigFactory}
import org.scalatest.{FlatSpec, MustMatchers}
import org.scalatest.EitherValues._
import collection.JavaConverters._

class NodeParserSpec extends FlatSpec with MustMatchers {

  private val id =
    "761d11916c0baf6632134cf5a55d3bdf821ee2e9f8b76ee4b7f8c7246d345fcf15099965c5f2d4adfaafbb9721202ee7b71eb3ccf1d96a1489f63506b498f1cb"

  behavior of "NodeParser"

  private def getConfigs(udpAddress: String, p2pAddress: String, capabilities: String): Set[Config] =
    ConfigFactory.parseString(s"""
       |nodes = [
       |    {
       |      discoveryUri = "$udpAddress"
       |      p2pUri = "$p2pAddress"
       |      capabilities = $capabilities
       |    }
       |  ]
      """.stripMargin).getConfigList("nodes").asScala.toSet

  it should "parse a Node" in {
    val configuredNodes = getConfigs("udp://127.0.0.1:1000", s"enode://$id@127.0.0.2:3000", "01")

    val parsed: Set[NodeInfo] = NodeParser.parseNodeInfos(configuredNodes)

    parsed.size mustBe 1
    val node = parsed.head
    node.discoveryAddress.getAddress.getAddress.toList mustBe List[Byte](127, 0, 0, 1)
    node.serverAddress.getAddress.getAddress.toList mustBe List[Byte](127, 0, 0, 2)
    node.discoveryAddress.getPort mustBe 1000
    node.serverAddress.getPort mustBe 3000
    node.capabilities.byte mustBe 0x1
  }

  it should "not parse a node with invalid discoveryUri" in {
    val config = getConfigs("://127.0.0.1:1000", s"enode://$id@127.0.0.2:3000", "01").head

    val parsed: Either[Set[NodeParser.Error], NodeInfo] = NodeParser.parseNodeInfo(config)

    parsed.left.value mustBe Set("Malformed URI for node ://127.0.0.1:1000")
  }

  it should "not parse a node with invalid p2pUri uri" in {
    val config = getConfigs("udp://127.0.0.1:1000", s"enod://$id@127.0.0.2:3000", "01").head

    val parsed: Either[Set[NodeParser.Error], NodeInfo] = NodeParser.parseNodeInfo(config)

    parsed.left.value mustBe Set("Invalid node scheme 'enod'. It should be 'enode'.")
  }

  it should "not parse a node with invalid p2p uri id" in {
    val invalidId = id.drop(120)
    val config = getConfigs("udp://127.0.0.1:1000", s"enode://$invalidId@127.0.0.2:3000", "01").head

    val parsed: Either[Set[NodeParser.Error], NodeInfo] = NodeParser.parseNodeInfo(config)

    parsed.left.value mustBe Set(s"Invalid id length for '$invalidId'. It should be 128.")
  }
}
