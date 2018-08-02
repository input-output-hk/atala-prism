package io.iohk.cef.discovery

import java.net.InetSocketAddress

import akka.{actor => untyped}
import akka.actor.typed.ActorSystem
import akka.io.Udp
import akka.testkit.typed.scaladsl.TestProbe
import akka.util.ByteString
import akka.{testkit => untypedKit}
import io.iohk.cef.discovery.DiscoveryListener._
import io.iohk.cef.network.encoding.{Decoder, Encoder}
import io.iohk.cef.network.{Capabilities, NodeInfo}
import org.scalamock.scalatest.MockFactory
import org.scalatest.{FlatSpec, MustMatchers}


class UDPBridgeSpec extends FlatSpec with MustMatchers with MockFactory {
  implicit val untypedSystem = untyped.ActorSystem("UDPBridge_system")
  implicit val system = ActorSystem.wrap(untypedSystem)

  class UdpTestHelper {
    val encoder = stub[Encoder[DiscoveryWireMessage, ByteString]]
    val decoder = stub[Decoder[ByteString, DiscoveryWireMessage]]
    val listenerInbox = TestProbe[DiscoveryListenerRequest]()

    val bridge = untypedKit.TestActorRef(new UDPBridge(listenerInbox.ref,
      encoder,
      decoder,
      _ => ()))

    val nodeAddr = new InetSocketAddress(1000)

    val socket = untypedKit.TestProbe("probe2")
    val node = NodeInfo(ByteString("1"), nodeAddr, nodeAddr, Capabilities(1))
    val addr = new InetSocketAddress(1000)
    bridge.tell(Udp.Bound(addr), socket.ref)
    listenerInbox.expectMessage(Forward(Ready(addr)))

  }

  behavior of "UDPBridge"

  it should "forward a received message" in new UdpTestHelper {
    val addr2 = new InetSocketAddress(1001)
    val data = Ping(1, node, 0L, ByteString("nonce"))
    val encodedData = ByteString("Ping")
    (decoder.decode _) when(encodedData) returns(data)
    bridge ! Udp.Received(encodedData, addr2)

    listenerInbox.expectMessage(Forward(MessageReceived(data, addr2)))
  }

  it should "forward a sent message" in new UdpTestHelper {
    val addr2 = new InetSocketAddress(1001)
    val data = Ping(1, node, 0L, ByteString("nonce"))
    val encodedData = ByteString("Ping")
    (encoder.encode _) when(data) returns(encodedData)
    bridge ! SendMessage(data, addr2)

    socket.expectMsg(Udp.Send(encodedData, addr2))
  }
}
