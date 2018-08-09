package io.iohk.cef.network.transport.tcp

import java.net.InetSocketAddress
import java.nio.ByteBuffer

import io.iohk.cef.network.encoding.StreamCodec
import io.iohk.cef.network.transport.tcp.NetUtils._
import org.scalatest.FlatSpec
import org.scalatest.Matchers._
import org.scalatest.concurrent.Eventually._

import scala.collection.mutable.ListBuffer

class TcpNetworkTransportSpec extends FlatSpec {

  behavior of "TcpNetworkTransport"

  it should "send and receive a message" in new AlicesConfig with BobsConfig {

    alicesTransport.sendMessage(bobsAddress, "Hello, Bob!")

    eventually {
      bobsInbox should contain("Hello, Bob!")
    }
  }

  private val codec = new StreamCodec[String, ByteBuffer](
    (t: String) => ByteBuffer.wrap(t.getBytes),
    (u: ByteBuffer) => Seq(new String(toArray(u)))
  )

  trait AlicesConfig {
    val alicesAddress: InetSocketAddress = aRandomAddress()
    val alicesInbox: ListBuffer[String] = new ListBuffer()
    val alicesTransport =
      new TcpNetworkTransport[String](logMessages(alicesInbox), codec, new NettyTransport(alicesAddress))
  }

  trait BobsConfig {
    val bobsAddress: InetSocketAddress = aRandomAddress()
    val bobsInbox: ListBuffer[String] = new ListBuffer()
    val bobsTransport =
      new TcpNetworkTransport[String](logMessages(bobsInbox), codec, new NettyTransport(bobsAddress))
  }
}
