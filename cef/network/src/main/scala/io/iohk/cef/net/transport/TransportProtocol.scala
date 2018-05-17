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
    * @return
    */
  def createTransport(): Behavior[TransportCommand]

  /**
    * TransportCommands create and configure transports
    * but do not connect or send messages.
    */
  sealed trait TransportCommand

  /**
    * Create an outbound connection to a peer on the network.
    * @param address the address to connect to
    * @param eventHandler an actor to send notifications about events on the connection.
    */
  case class Connect(address: AddressType,
                     eventHandler: ActorRef[ConnectionEvent])
      extends TransportCommand

  /**
    * Create a listener on a given (presumably local) address,
    * providing a function to spin up handlers for each inbound
    * connection.
    * @param address The address on which to listen.
    * @param listener An actor to receive success or failure notification.
    * @param connectionFactory When inbound connections are received by
    *                          the listener will use this function to spin up your actor
    *                          for handling events on that connection, passing the
    *                          peer's address.
    */
  case class CreateListener(address: AddressType,
                            listener: ActorRef[ListenerEvent],
                            connectionFactory: AddressType => ActorRef[ConnectionEvent])
      extends TransportCommand

  /**
    * ListenerCommand supports the setup of listeners on a specific address.
    */
  sealed trait ListenerCommand

  // TODO unbind a listener

  /**
    * ListenerEvent defines notifications sent by listeners to the user.
    */
  sealed trait ListenerEvent

  /**
    * Indicates that a listener is setup.
    * @param address the local address on which the listener is listening.
    */
  case class Listening(address: AddressType) extends ListenerEvent

  /**
    * ConnectionEvent defines notifications of events on established p2p channels.
    */
  sealed trait ConnectionEvent

  /**
    * Notification of a new connection between peers on the network.
    * @param address The remote address.
    * @param connection An actor which can be used to control the connection.
    */
  case class Connected(address: AddressType, connection: ActorRef[ConnectionCommand])
    extends ConnectionEvent

  /**
    * Notification that setting up a connection has failed.
    * @param message a human readable message.
    * @param address the remote address.
    */
  case class ConnectionError(message: String, address: AddressType)
    extends ConnectionEvent

  /**
    * Notification that a message has been received from a peer on the network.
    * @param message the received message
    */
  case class MessageReceived(message: MessageType) extends ConnectionEvent
  // TODO connection events
  // connection closed

  /**
    * ConnectionCommand allows the management of connections and message sending.
    */
  sealed trait ConnectionCommand

  /**
    * Send a message to a connected peer.
    * @param message the message to send.
    */
  case class SendMessage(message: MessageType) extends ConnectionCommand

  // TODO close connection

}

object TransportProtocol {}
