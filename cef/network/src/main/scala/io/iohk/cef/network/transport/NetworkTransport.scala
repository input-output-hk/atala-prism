package io.iohk.cef.network.transport

object NetworkTransport {
  class TransportInitializationException extends RuntimeException
}

/**
  * NetworkTransports define a p2p abstraction over TCP, TLS, UDP, RLPx, etc.
  *
  * @param messageHandler The messageHandler receives inbound messages from peers.
  */
abstract class NetworkTransport[Address, Message](messageHandler: (Address, Message) => Unit) {

  /**
    * Send a message to another peer.
    *
    * @param address the address of the peer to which to send the message
    * @param message the message body itself.
    */
  def sendMessage(address: Address, message: Message): Unit
}
