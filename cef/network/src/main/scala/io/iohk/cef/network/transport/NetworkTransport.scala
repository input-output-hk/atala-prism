package io.iohk.cef.network.transport

/**
  * TransportProtocol defines an abstraction over network transports,
  * such as TCP, TLS, UDP or RLPx.
  */
abstract class TransportProtocol3[NodeId, Message](
    /**
      * The messageHandler receives inbound messages from peers.
      */
    messageHandler: (NodeId, Message) => Unit) {

  class TransportInitializationException extends RuntimeException

  /**
    * The primary purpose of this method is to decouple the creation
    * and the turning on of a transport.
    *
    * This method should make the transport ready
    * for sending and receiving messages. This might mean, for example,
    * binding to an address.
    *
    * @throws TransportInitializationException if initialization fails.
    */
  def start(): Unit

  def sendMessage(nodeId: NodeId, message: Message): Unit
}
