package io.iohk.cef.network.transport.tcp

import java.io.OutputStream
import java.net.{InetSocketAddress, ServerSocket, Socket}

import io.iohk.cef.network.encoding.{Encoder, StreamDecoder}
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
      new TcpNetworkTransport(discardMessages, testEncoder, testDecoder, TcpNetworkConfiguration(alicesAddress))

    an[UnsupportedOperationException] shouldBe thrownBy(alice.sendMessage(bobsAddress, ""))
  }

  it should "start" in {
    val address = aRandomAddress()
    val transport =
      new TcpNetworkTransport(discardMessages, testEncoder, testDecoder, TcpNetworkConfiguration(address))
    isListening(address) shouldBe false

    transport.start()

    isListening(address) shouldBe true
  }

  it should "receive a message" in {
    val address = aRandomAddress()
    val messagesReceived = new mutable.ListBuffer[String]()
    val transport = new TcpNetworkTransport[String](logMessages(messagesReceived), testEncoder, testDecoder,
      TcpNetworkConfiguration(address)).start()

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
      new TcpNetworkTransport(discardMessages, testEncoder, testDecoder, TcpNetworkConfiguration(alicesAddress)).start()

    val bob =
      new TcpNetworkTransport(logMessages(bobsMessages), testEncoder, testDecoder, TcpNetworkConfiguration(bobsAddress)).start()

    alice.sendMessage(bobsAddress, "Hello, Bob!")

    eventually {
      bobsMessages should contain("Hello, Bob!")
    }
  }

  def writeTo(address: InetSocketAddress, bytes: Array[Byte]): Unit = {
    val socket = new Socket(address.getHostName, address.getPort)
    val out: OutputStream = socket.getOutputStream
    try {
      out.write(bytes)
    }
    finally {
        out.close()
    }
  }

  private def aRandomAddress(): InetSocketAddress = {
    val s = new ServerSocket(0)
    try {
      new InetSocketAddress("localhost", s.getLocalPort)
    } finally {
      s.close()
    }
  }

  private def discardMessages[T](remoteAddress: InetSocketAddress, message: T): Unit = ()

  private def logMessages[T](messages: mutable.ListBuffer[T])(remoteAddress: InetSocketAddress, message: T): Unit =
    messages += message

  private def isListening(address: InetSocketAddress): Boolean = {
    try {
      new Socket(address.getHostName, address.getPort).close()
      true
    } catch {
      case e: Exception =>
        false
    }
  }

  private def testDecoder = new StreamDecoder[ByteBuf, String] {
    override def decodeStream(u: ByteBuf): Seq[String] = Seq(decode(u))

    override def decode(u: ByteBuf): String = u.toString(io.netty.util.CharsetUtil.UTF_8)
  }

  private def testEncoder = new Encoder[String, ByteBuf] {
    override def encode(t: String): ByteBuf = {
      val buff = Unpooled.directBuffer()
      buff.writeBytes(t.getBytes)
    }
  }
}
