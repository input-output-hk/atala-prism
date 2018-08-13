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
    * Message forwarding.
    * When receiving wrapped messages that are not for this node
    * we should examine the routing table to see if any peer matches
    * the message destination. If not, forward the message to 'suitable'
    * peers in the routing table.
    */
  /**
    * The primary purpose of this method is to decouple the configuration
    * and the 'turning on' of a network.
    *
    * After successful return from this method, the network
    * instance returned will be ready to send and receive messages.
    *
    * This method runs synchronously and return the new network state.
    *
    */
  def start(): ConversationalNetwork[Message] = ???

  /**
    * Send a message to another network address.
    *
    * The process for sending a message is
    * 1. Wrap the caller's message, setting src and dst headers.
    * 2. Examine the routing table to see if an existing peer has the given address.
    *    If so, send the message directly to that peer.
    * 3. Otherwise, send the message to 'suitable' peers in the routing table.
    * @param address the address of the peer to which to send the message
    * @param message the message body itself.
    */
  def sendMessage(address: NetworkAddress, message: Message): Unit = ???
}
