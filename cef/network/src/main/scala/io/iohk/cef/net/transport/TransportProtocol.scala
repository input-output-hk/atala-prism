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
  type MessageType

  /**
    * This function bootstraps the transport and from there on
    * the message types dictate the allowable protocol.
    * [TODO verify this assertion carefully].
    * @return
    */
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

  case class Connected[PeerInfoType, MessageType](peerInfo: PeerInfoType, connection: ActorRef[ConnectionCommand[MessageType]])
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
                                                address: AddressType,
                                                replyTo: ActorRef[ListenerEvent[AddressType]])
    extends ListenerCommand[AddressType, PeerInfoType]

  /**
   * ListenerEvent defines notifications sent by listeners to the user.
   */
  sealed trait ListenerEvent[AddressType]

  case class Listening[AddressType](address: AddressType)
      extends ListenerEvent[AddressType]

  case class ConnectionReceived[AddressType, MessageType](address: AddressType, connection: ActorRef[ConnectionCommand[MessageType]])
      extends ListenerEvent[AddressType]
  //  case class Close[PeerInfoType](peerInfo: PeerInfoType) extends ListenerEvent
  //  case class Error(message: String) extends ListenerEvent
  //

  sealed trait ConnectionCommand[MessageType]

  case class SendMessage[MessageType](message: MessageType) extends ConnectionCommand[MessageType]
}
