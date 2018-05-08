package io.iohk.cef.net.transport

import akka.actor.typed.{ActorRef, Behavior}
import io.iohk.cef.net.transport.TransportProtocol.TransportCommand

trait TransportProtocol {

  type AddressType
  type PeerInfoType

  def createTransport(): Behavior[TransportCommand[AddressType, PeerInfoType]]

}

object TransportProtocol {

  sealed trait TransportCommand[AddressType, PeerInfoType]

  case class Connect[AddressType, PeerInfoType](
                                                 address: AddressType,
                                                 replyTo: ActorRef[ConnectionReply[PeerInfoType]])
    extends TransportCommand[AddressType, PeerInfoType]

  case class CreateListener[AddressType, PeerInfoType](replyTo: ActorRef[ListenerCreated])
    extends TransportCommand[AddressType, PeerInfoType]

  //case class GetListeners(replyTo: ActorRef)
  sealed trait ConnectionReply[PeerInfoType]

  case class Connected[PeerInfoType](peerInfo: PeerInfoType)
    extends ConnectionReply[PeerInfoType]

  case class ConnectionError[PeerInfoType](message: String, peerInfo: PeerInfoType)
    extends ConnectionReply[PeerInfoType]

  case class ListenerCreated(listener: ActorRef[ListenerCommand])

  sealed trait ListenerEvent
  case class Listening[AddressType](address: AddressType) extends ListenerEvent
  //  case class ConnectionReceived[PeerInfoType](peerInfo: PeerInfoType)
  //      extends ListenerEvent
  //  case class Close[PeerInfoType](peerInfo: PeerInfoType) extends ListenerEvent
  //  case class Error(message: String) extends ListenerEvent
  //
  sealed trait ListenerCommand
  case class Listen[AddressType](addressType: AddressType, replyTo: ActorRef[ListenerEvent]) extends ListenerCommand
  //  case class GetListenerAddr[AddressType](replyTo: ActorRef[AddressType])
  //      extends ListenerMessage

}