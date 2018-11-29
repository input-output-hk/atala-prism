package io.iohk.cef.network
import io.iohk.cef.network.discovery.NetworkDiscovery
import io.iohk.cef.codecs.nio.NioCodec
import io.iohk.cef.network.transport.Transports
import scala.reflect.runtime.universe._
import io.iohk.cef.network.transport.Frame

/**
  * Represents a (lightweight) resource for type-safe sending, receiving and disseminating messages.
  * @param transports an application wide instance that holds the actual network resources.
  * @tparam Message the user message type.
  */
class Network[Message: TypeTag](networkDiscovery: NetworkDiscovery, transports: Transports)(
    implicit enc: NioCodec[Frame[Message]]) {

  private val conversationalNetwork = new ConversationalNetwork[Message](networkDiscovery, transports)
  private val disseminationalNetwork = new DisseminationalNetwork[Message](networkDiscovery, transports)

  def sendMessage(nodeId: NodeId, message: Message): Unit =
    conversationalNetwork.sendMessage(nodeId, message)

  def disseminateMessage(message: Message): Unit =
    disseminationalNetwork.disseminateMessage(message)

  def messageStream: MessageStream[Message] = conversationalNetwork.messageStream
}
