package io.iohk.cef.net.transport

import akka.actor.typed.{ActorRef, Behavior}

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
  def createTransport(): Behavior[TransportCommand]

  /**
    * TransportCommands create and configure transports
    * but do not connect or send messages.
    */
  sealed trait TransportCommand

  case class Connect(address: AddressType,
                     replyTo: ActorRef[ConnectionReply],
                     listener: ActorRef[ConnectionEvent])
      extends TransportCommand

  case class CreateListener(address: AddressType,
                            listener: ActorRef[ListenerEvent])
      extends TransportCommand

  /**
    * ConnectionReply allows connection oriented transports
    * to notify users re the success of Connect commands.
    */
  sealed trait ConnectionReply

  case class Connected(address: AddressType,
                       connection: ActorRef[ConnectionCommand])
      extends ConnectionReply

  case class ConnectionError(message: String, address: AddressType)
      extends ConnectionReply

  /**
    * ListenerCommand supports the setup of listeners on a specific address.
    */
  sealed trait ListenerCommand

  // TODO unbind a listener

  /**
    * ListenerEvent defines notifications sent by listeners to the user.
    */
  sealed trait ListenerEvent

  case class ListenerCreated(listener: ActorRef[ListenerCommand])
      extends ListenerEvent

  case class Listening(address: AddressType) extends ListenerEvent

  case class ConnectionReceived(address: AddressType,
                                connection: ActorRef[ConnectionCommand])
      extends ListenerEvent

  /**
    * ConnectionEvent defines notifications of events on established p2p channels.
    */
  sealed trait ConnectionEvent

  //  case class Connected[AddressType, MessageType](address: AddressType, connection: ActorRef[ConnectionCommand[MessageType]])
  //    extends ConnectionEvent[AddressType, MessageType]
  //
  //  case class ConnectionError[AddressType](message: String,
  //                                          address: AddressType)
  //    extends ConnectionReply

  case class MessageReceived(message: MessageType) extends ConnectionEvent
  // TODO connection events
  // connection closed

  sealed trait ConnectionCommand

  case class SendMessage(message: MessageType) extends ConnectionCommand

  // TODO close connection

}

object TransportProtocol {}
