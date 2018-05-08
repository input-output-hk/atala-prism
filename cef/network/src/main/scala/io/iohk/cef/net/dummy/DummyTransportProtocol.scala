package io.iohk.cef.net.dummy

import java.util.UUID

import akka.actor.typed.{Behavior, ActorRef}
import akka.actor.typed.scaladsl.Behaviors
import io.iohk.cef.net.dummy.DummyTransportProtocol._
import io.iohk.cef.net.transport.TransportProtocol
import io.iohk.cef.net.transport.TransportProtocol._

class DummyTransportProtocol extends TransportProtocol {

  type AddressType = DummyAddress
  type PeerInfoType = DummyPeerInfo

  def createTransport(): Behavior[TransportCommand[AddressType, PeerInfoType]] =
    dummyTransport(Nil, Nil)


  def dummyTransport(peers: List[DummyPeerInfo], listeners: List[ActorRef[Behavior[ListenerCommand]]]): Behavior[TransportCommand[DummyAddress, DummyPeerInfo]] = Behaviors.receive {
    (context, message) =>
      message match {
        case Connect(address, replyTo) =>
          val newPeer = DummyPeerInfo(address.toString)

          if (address.to != "localhost") {
            replyTo ! ConnectionError("Cannot connect to remote addresses. I'm a dummy.", newPeer)
            dummyTransport(peers, listeners)
          } else {
            replyTo ! Connected(newPeer)
            dummyTransport(newPeer :: peers, listeners)
          }

        case CreateListener(replyTo) =>
          val listenerActor: ActorRef[ListenerCommand] = context.spawn(listenerBehaviour, s"listener_${UUID.randomUUID()}")
          val createdMessage = ListenerCreated(listenerActor)
          replyTo ! createdMessage
          dummyTransport(peers, listeners)
      }
  }

  val listenerBehaviour: Behavior[ListenerCommand] = Behaviors.receive {
    (context, message) =>
      message match {
        case Listen(address, replyTo) => ???
      }
  }
}

object DummyTransportProtocol {
  case class DummyPeerInfo(name: String)
  case class DummyAddress(to: String)
}