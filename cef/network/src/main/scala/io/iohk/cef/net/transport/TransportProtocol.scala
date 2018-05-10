package io.iohk.cef.net.transport

import akka.actor.typed.{ActorRef, Behavior}
import io.iohk.cef.net.transport.TransportProtocol.TransportCommand

/**
  * TransportProtocol defines an abstraction over network transports,
  * such as TCP, TLS or RLPx.
  */
trait TransportProtocol {

  type AddressType
  type PeerInfoType

  def createTransport(): Behavior[TransportCommand[AddressType, PeerInfoType]]

}

object TransportProtocol {

  /**
   * TransportCommands create and configure transports
   * but do not connect or send messages.
   */
  sealed trait TransportCommand[AddressType, PeerInfoType]

  case class Connect[AddressType, PeerInfoType](
      address: AddressType,
      replyTo: ActorRef[ConnectionReply[PeerInfoType]])
      extends TransportCommand[AddressType, PeerInfoType]

  case class CreateListener[AddressType, PeerInfoType](
      replyTo: ActorRef[ListenerCreated[AddressType, PeerInfoType]])
      extends TransportCommand[AddressType, PeerInfoType]


  /**
   * ConnectionReply allows connection oriented transports
   * to notify users re the success of Connect commands.
   */
  sealed trait ConnectionReply[PeerInfoType]

  case class Connected[PeerInfoType](peerInfo: PeerInfoType)
      extends ConnectionReply[PeerInfoType]

  case class ConnectionError[PeerInfoType](message: String,
                                           peerInfo: PeerInfoType)
      extends ConnectionReply[PeerInfoType]

  /**
   * ListenerCreated allows for the setup of listeners to be notified.
   */
  case class ListenerCreated[AddressType, PeerInfoType](
      listener: ActorRef[ListenerCommand[AddressType, PeerInfoType]])


  /**
   * ListenerCommand supports the setup of listeners on a specific address.
   */
  sealed trait ListenerCommand[AddressType, PeerInfoType]

  case class Listen[AddressType, PeerInfoType](
                                                addressType: AddressType,
                                                replyTo: ActorRef[ListenerEvent[AddressType, PeerInfoType]])
    extends ListenerCommand[AddressType, PeerInfoType]


  /**
   * ListenerEvent defines notifications sent by listeners to the user.
   */
  sealed trait ListenerEvent[AddressType, PeerInfoType]

  case class Listening[AddressType, PeerInfoType](address: AddressType)
      extends ListenerEvent[AddressType, PeerInfoType]

  case class ConnectionReceived[AddressType, PeerInfoType](peerInfo: PeerInfoType)
      extends ListenerEvent[AddressType, PeerInfoType]
  //  case class Close[PeerInfoType](peerInfo: PeerInfoType) extends ListenerEvent
  //  case class Error(message: String) extends ListenerEvent
  //



}
