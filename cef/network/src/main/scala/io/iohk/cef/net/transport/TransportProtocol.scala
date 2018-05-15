package io.iohk.cef.net.transport

import akka.actor.typed.{ActorRef, Behavior}
import io.iohk.cef.net.transport.TransportProtocol.TransportCommand

/**
  * TransportProtocol defines an abstraction over network transports,
  * such as TCP, TLS or RLPx.
  */
trait TransportProtocol {

  type AddressType
  type MessageType

  /**
    * This function bootstraps the transport and from there on
    * the message types dictate the allowable protocol.
    * [TODO verify this assertion carefully].
    * @return
    */
  def createTransport(): Behavior[TransportCommand[AddressType]]

}

object TransportProtocol {

  /**
   * TransportCommands create and configure transports
   * but do not connect or send messages.
   */
  sealed trait TransportCommand[AddressType]

  case class Connect[AddressType](
      address: AddressType,
      replyTo: ActorRef[ConnectionReply])
      extends TransportCommand[AddressType]

  case class CreateListener[AddressType](
      address: AddressType,
      replyTo: ActorRef[ListenerEvent[AddressType]])
      extends TransportCommand[AddressType]


  /**
   * ConnectionReply allows connection oriented transports
   * to notify users re the success of Connect commands.
   */
  sealed trait ConnectionReply

  case class Connected[AddressType, MessageType](address: AddressType, connection: ActorRef[ConnectionCommand[MessageType]])
      extends ConnectionReply

  case class ConnectionError[AddressType](message: String,
                                          address: AddressType)
      extends ConnectionReply


  /**
   * ListenerCommand supports the setup of listeners on a specific address.
   */
  sealed trait ListenerCommand[AddressType]

  // TODO unbind a listener

  /**
   * ListenerEvent defines notifications sent by listeners to the user.
   */
  sealed trait ListenerEvent[AddressType]

  case class ListenerCreated[AddressType](listener: ActorRef[ListenerCommand[AddressType]]) extends ListenerEvent[AddressType]

  case class Listening[AddressType](address: AddressType)
      extends ListenerEvent[AddressType]

  case class ConnectionReceived[AddressType, MessageType](address: AddressType, connection: ActorRef[ConnectionCommand[MessageType]])
      extends ListenerEvent[AddressType]

  /**
    * ConnectionEvent defines notifications of events on established p2p channels.
    */
  sealed trait ConnectionEvent[MessageType]

  case class MessageReceived[MessageType](message: MessageType) extends ConnectionEvent[MessageType]
  // TODO connection events
  // connection closed

  sealed trait ConnectionCommand[MessageType]

  case class SendMessage[MessageType](message: MessageType) extends ConnectionCommand[MessageType]

  // TODO close connection
}
