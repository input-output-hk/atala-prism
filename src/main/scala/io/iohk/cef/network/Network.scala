package io.iohk.cef.network
import io.iohk.cef.network.discovery.NetworkDiscovery
import io.iohk.cef.network.encoding.nio.{NioDecoder, NioEncoder}
import io.iohk.cef.network.transport.{StreamLike, Transports}

/**
  * Represents a (lightweight) resource for type-safe sending, receiving and disseminating messages.
  * @param transports an application wide instance that holds the actual network resources.
  * @tparam Message the user message type.
  */
class Network[Message: NioEncoder: NioDecoder](networkDiscovery: NetworkDiscovery, transports: Transports) {

  private val conversationalNetwork = new ConversationalNetwork[Message](networkDiscovery, transports)
  private val disseminationalNetwork = new DisseminationalNetwork[Message](networkDiscovery, transports)

  def sendMessage(nodeId: NodeId, message: Message): Unit =
    conversationalNetwork.sendMessage(nodeId, message)

  def disseminateMessage(message: Message): Unit =
    disseminationalNetwork.disseminateMessage(message)

  def messageStream: StreamLike[Message] = conversationalNetwork.messageStream
}
