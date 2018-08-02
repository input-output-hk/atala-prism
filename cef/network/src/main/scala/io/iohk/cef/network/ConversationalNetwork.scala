package io.iohk.cef.network

/**
  * Represents a conversational model of the network
  * where peers exchange messages in a point to point
  * fashion.
  *
  * The term 'conversational' is used in the sense introduced
  * by Van Jacobson, to distinguish from the 'disseminational'
  * networking style.
  */
class ConversationalNetwork[Message](
    /**
      * The messageHandler receives inbound messages from other network addresses.
      */
    messageHandler: (NetworkAddress, Message) => Unit) {

  /**
    * The primary purpose of this method is to decouple the configuration
    * and the 'turning on' of a network.
    *
    * After successful return from this method, the network
    * is ready to send and receive messages.
    *
    * This method runs synchronously.
    *
    */
  def start(): Unit = ???

  /**
    * Send a message to another network address.
    * @param address the address of the peer to which to send the message
    * @param message the message body itself.
    */
  def sendMessage(address: NetworkAddress, message: Message): Unit = ???
}
