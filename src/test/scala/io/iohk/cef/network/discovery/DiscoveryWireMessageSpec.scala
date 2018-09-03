package io.iohk.cef.network.discovery

import java.net.{InetAddress, InetSocketAddress}

import akka.util.ByteString
import io.iohk.cef.network.{Capabilities, NodeInfo}
import org.scalatest.{FlatSpec, MustMatchers}

class DiscoveryWireMessageSpec extends FlatSpec with MustMatchers {

  behavior of "DiscoveryWireMessage"

  it should "not lose info when encode/decode the messages" in {
    val addr1 = new InetSocketAddress(InetAddress.getByAddress(Array(1, 2, 3, 4)), 5)
    val addr2 = new InetSocketAddress(InetAddress.getByAddress(Array(6, 7, 8, 9)), 10)
    val node = NodeInfo(ByteString("node"), addr1, addr2, Capabilities(2))

    import io.iohk.cef.network.encoding.rlp.RLPImplicits._

    val ping = Ping(1, node, 3, ByteString("2"))
    Ping.pingRLPEncDec.decode(Ping.pingRLPEncDec.encode(ping)) mustBe ping

    val pong = Pong(node, ByteString("1"), 2)
    Pong.pongRLPEncDec.decode(Pong.pongRLPEncDec.encode(pong)) mustBe pong

    val neighbors = Neighbors(Capabilities(1), ByteString("token"), 2, Seq(node, node), 3)
    Neighbors.neighborsRLPEncDec.decode(Neighbors.neighborsRLPEncDec.encode(neighbors)) mustBe neighbors

    val seek = Seek(Capabilities(2), 3, 4, ByteString("nonce"))
    Seek.seekRLPEncDec.decode(Seek.seekRLPEncDec.encode(seek)) mustBe seek

    Seq(ping, pong, neighbors, seek).foreach { m =>
      DiscoveryWireMessage.RLPEncDec.decode(DiscoveryWireMessage.RLPEncDec.encode(m)) mustBe m
    }
  }
}
