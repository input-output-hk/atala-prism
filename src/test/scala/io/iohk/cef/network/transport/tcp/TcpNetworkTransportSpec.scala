package io.iohk.cef.network.transport.tcp

import java.net.InetSocketAddress

import io.iohk.cef.network.encoding.nio._
import io.iohk.cef.network.transport.tcp.NetUtils._

import org.scalatest.FlatSpec
import org.scalatest.Matchers._
import org.scalatest.concurrent.Eventually._

import scala.collection.mutable.ListBuffer

import scala.concurrent.duration._

class TcpNetworkTransportSpec extends FlatSpec {

  private implicit val patienceConfig = PatienceConfig(timeout = 1 second)

  behavior of "TcpNetworkTransport"

  it should "send and receive a message" in new AlicesConfig with BobsConfig {

    alicesTransport.sendMessage(bobsAddress, Message("Hello, Bob!"))

    eventually {
      bobsInbox should contain(Message("Hello, Bob!"))
    }
  }

  case class Message(content: String)

  trait AlicesConfig {
    val alicesAddress: InetSocketAddress = aRandomAddress()
    val alicesInbox: ListBuffer[Message] = new ListBuffer()
    val alicesTransport =
      new TcpNetworkTransport(logMessages(alicesInbox), new NettyTransport(alicesAddress))
  }

  trait BobsConfig {
    val bobsAddress: InetSocketAddress = aRandomAddress()
    val bobsInbox: ListBuffer[Message] = new ListBuffer()
    val bobsTransport =
      new TcpNetworkTransport(logMessages(bobsInbox), new NettyTransport(bobsAddress))
  }
}
