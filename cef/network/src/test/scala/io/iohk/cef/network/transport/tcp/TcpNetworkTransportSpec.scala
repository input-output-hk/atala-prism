package io.iohk.cef.network.transport.tcp

import io.iohk.cef.network.encoding.StreamCodec
import io.iohk.cef.network.transport.tcp.NetUtils._
import io.netty.buffer.{ByteBuf, Unpooled}
import org.scalatest.FlatSpec
import org.scalatest.Matchers._
import org.scalatest.concurrent.Eventually._

import scala.collection.mutable

class TcpNetworkTransportSpec extends FlatSpec {

  behavior of "TcpNetworkTransport"

  it should "not support messaging before starting" in {
    val alicesAddress = aRandomAddress()
    val bobsAddress = aRandomAddress()
    val alice =
      new TcpNetworkTransport[String](discardMessages,
                                      codec,
                                      TcpTransportConfiguration(alicesAddress))

    an[UnsupportedOperationException] shouldBe thrownBy(alice.sendMessage(bobsAddress, ""))
  }

  it should "start" in {
    val address = aRandomAddress()
    val transport =
      new TcpNetworkTransport(discardMessages, codec, TcpTransportConfiguration(address))
    isListening(address) shouldBe false

    transport.start()

    isListening(address) shouldBe true
  }

  it should "receive a message" in {
    val address = aRandomAddress()
    val messagesReceived = new mutable.ListBuffer[String]()

    new TcpNetworkTransport[String](logMessages(messagesReceived),
                                                    codec,
                                                    TcpTransportConfiguration(address)).start()

    writeTo(address, "hello".getBytes)

    eventually {
      messagesReceived should contain("hello")
    }
  }

  it should "send a message to a valid address" in {

    val alicesAddress = aRandomAddress()
    val bobsAddress = aRandomAddress()
    val bobsMessages = new mutable.ListBuffer[String]()

    val alice =
      new TcpNetworkTransport(discardMessages, codec, TcpTransportConfiguration(alicesAddress))
        .start()

    val bob =
      new TcpNetworkTransport(logMessages(bobsMessages),
                              codec,
                              TcpTransportConfiguration(bobsAddress)).start()

    alice.sendMessage(bobsAddress, "Hello, Bob!")

    eventually {
      bobsMessages should contain("Hello, Bob!")
    }
  }

  private val codec = StreamCodec[String, ByteBuf](
    (t: String) => Unpooled.directBuffer().writeBytes(t.getBytes),
    (u: ByteBuf) => Seq(u.toString(io.netty.util.CharsetUtil.UTF_8))
  )
}
