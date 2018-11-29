package io.iohk.cef.network
import io.iohk.cef.network.discovery.NetworkDiscovery
import io.iohk.cef.codecs.nio.NioEncDec
import io.iohk.cef.network.transport.Transports
import scala.reflect.runtime.universe._
import io.iohk.cef.network.transport.Frame

/**
  * Represents a (lightweight) resource for type-safe sending, receiving and disseminating messages.
  * @param transports an application wide instance that holds the actual network resources.
  * @tparam Message the user message type.
  */
trait Network[Message] {

  def sendMessage(nodeId: NodeId, message: Message): Unit

  def disseminateMessage(message: Message): Unit

  def messageStream: MessageStream[Message]
}

object Network {
  def apply[Message: WeakTypeTag](networkDiscovery: NetworkDiscovery, transports: Transports)(
      implicit enc: NioEncDec[Frame[Message]]
  ): Network[Message] =
    new NetworkImpl[Message](networkDiscovery, transports)
}

class NetworkImpl[Message: WeakTypeTag](networkDiscovery: NetworkDiscovery, transports: Transports)(
    implicit enc: NioEncDec[Frame[Message]])
    extends Network[Message] {

  private val conversationalNetwork = new ConversationalNetwork[Message](networkDiscovery, transports)
  private val disseminationalNetwork = new DisseminationalNetwork[Message](networkDiscovery, transports)

  override def sendMessage(nodeId: NodeId, message: Message): Unit =
    conversationalNetwork.sendMessage(nodeId, message)

  override def disseminateMessage(message: Message): Unit =
    disseminationalNetwork.disseminateMessage(message)

  override def messageStream: MessageStream[Message] = conversationalNetwork.messageStream
}
