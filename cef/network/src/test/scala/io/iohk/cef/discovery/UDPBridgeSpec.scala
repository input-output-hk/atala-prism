package io.iohk.cef.discovery

import java.net.InetSocketAddress

import akka.actor.ActorSystem
import akka.io.Udp
import akka.testkit.typed.scaladsl.TestInbox
import akka.util.ByteString
import akka.{testkit => untyped}
import io.iohk.cef.discovery.DiscoveryListener.{DiscoveryListenerRequest, Forward, Ready}
import io.iohk.cef.encoding.{Decoder, Encoder}
import org.scalatest.{FlatSpec, MustMatchers}

class UDPBridgeSpec extends FlatSpec with MustMatchers {


  import io.iohk.cef.encoding.rlp.RLPEncoders._
  import io.iohk.cef.encoding.rlp.RLPImplicits._

  val encoder = implicitly[Encoder[DiscoveryWireMessage, ByteString]]

  val decoder = implicitly[Decoder[ByteString, DiscoveryWireMessage]]
  implicit val system = ActorSystem("UDPBridgeSpec_System")

  behavior of "UDPBridge"

  it should "forward ready when bound" in {
    val listenerInbox = TestInbox[DiscoveryListenerRequest]()
    val bridge = untyped.TestActorRef(new UDPBridge(listenerInbox.ref,
      encoder,
      decoder,
      _ => ()))
    val addr = new InetSocketAddress(1000)
    bridge ! Udp.Bound(addr)
    listenerInbox.expectMessage(Forward(Ready(addr)))
  }
}
