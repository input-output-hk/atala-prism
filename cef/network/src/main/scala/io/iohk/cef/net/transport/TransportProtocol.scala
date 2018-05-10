package io.iohk.cef.net.transport

import akka.actor.typed.{ActorRef, Behavior}
import io.iohk.cef.net.transport.TransportProtocol.TransportMessage

trait TransportProtocol {

  type AddressType
  type PeerInfoType

  def createTransport(): Behavior[TransportMessage[AddressType, PeerInfoType]]
//  case class CreateListener[AddressType, PeerInfoType](
//      address: AddressType,
//      connectionEventHandler: ActorRef[ListenerEvent],
//      replyTo: ActorRef[ActorRef[ListenerMessage]])
//      extends TransportMessage[AddressType, PeerInfoType]
//
//  sealed trait ListenerEvent
//  case class Listening[AddressType](address: AddressType) extends ListenerEvent
//  case class ConnectionReceived[PeerInfoType](peerInfo: PeerInfoType)
//      extends ListenerEvent
//  case class Close[PeerInfoType](peerInfo: PeerInfoType) extends ListenerEvent
//  case class Error(message: String) extends ListenerEvent
//
//  sealed trait ListenerMessage
//  case class GetListenerAddr[AddressType](replyTo: ActorRef[AddressType])
//      extends ListenerMessage

}

object TransportProtocol {

  sealed trait TransportMessage[AddressType, PeerInfoType]

  case class Connect[AddressType, PeerInfoType](
                                                 address: AddressType,
                                                 replyTo: ActorRef[ConnectionReply[PeerInfoType]])
    extends TransportMessage[AddressType, PeerInfoType]

  //case class GetListeners(replyTo: ActorRef)
  sealed trait ConnectionReply[PeerInfoType]

  case class Connected[PeerInfoType](peerInfo: PeerInfoType)
    extends ConnectionReply[PeerInfoType]

  case class ConnectionError[PeerInfoType](message: String)
    extends ConnectionReply[PeerInfoType]

}