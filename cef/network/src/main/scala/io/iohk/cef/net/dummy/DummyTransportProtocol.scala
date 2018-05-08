package io.iohk.cef.net.dummy

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import io.iohk.cef.net.dummy.DummyTransportProtocol._
import io.iohk.cef.net.transport.TransportProtocol
import io.iohk.cef.net.transport.TransportProtocol._

class DummyTransportProtocol extends TransportProtocol {

  type AddressType = DummyAddress
  type PeerInfoType = DummyPeerInfo

  def createTransport(): Behavior[TransportMessage[AddressType, PeerInfoType]] =
    dummyTransport(Nil)


  def dummyTransport(peers: List[DummyPeerInfo]): Behavior[TransportMessage[DummyAddress, DummyPeerInfo]] = Behaviors.receive {
    (_, message) =>
      message match {
        case Connect(address, replyTo) =>
          val newPeer = DummyPeerInfo(address.toString)

          if (address.to != "localhost") {
            replyTo ! ConnectionError("Cannot connect to remote addresses. I'm a dummy.", newPeer)
            dummyTransport(peers)
          } else {
            replyTo ! Connected(newPeer)
            dummyTransport(newPeer :: peers)
          }

//        case CreateListener(_, _, _) => ???
      }
  }
}

object DummyTransportProtocol {
  case class DummyPeerInfo(name: String)
  case class DummyAddress(to: String)
}