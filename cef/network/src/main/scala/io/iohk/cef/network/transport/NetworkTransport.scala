package io.iohk.cef.network.transport

object NetworkTransport {
  class TransportInitializationException extends RuntimeException
}

/**
  * NetworkTransports define a p2p abstraction over TCP, TLS, UDP, RLPx, etc.
  */
abstract class NetworkTransport[Address, Message](
    /**
      * The messageHandler receives inbound messages from peers.
      */
    messageHandler: (Address, Message) => Unit) {

  /**
    * Although not specified here, you probably want your transport
    * implementation to accept a Codec in its constructor.
    * This is not specified here, however, in case some impls want to decode down to
    * Array[Byte] whilst others to netty bytebuf or akka ByteString, etc.
    */


  /**
    * The primary purpose of this method is to decouple the creation
    * and the 'turning on' of a transport.
    *
    * This method should make the transport ready
    * for sending and receiving messages. This might mean, for example,
    * binding to an address.
    *
    * This method should run synchronously.
    *
    * @throws TransportInitializationException if initialization fails.
    */
  def start(): Unit

  /**
    * Send a message to another peer.
    * @param address the address of the peer to which to send the message
    * @param message the message body itself.
    */
  def sendMessage(address: Address, message: Message): Unit
}
