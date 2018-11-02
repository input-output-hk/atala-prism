package io.iohk.cef.network
import io.iohk.cef.network.discovery.NetworkDiscovery
import io.iohk.cef.codecs.nio.{NioDecoder, NioEncoder}
import io.iohk.cef.network.transport.Transports
import scala.reflect.runtime.universe._
import io.iohk.cef.network.transport.Frame

class DisseminationalNetwork[Message: /*NioEncoder: NioDecoder:*/ WeakTypeTag](
    networkDiscovery: NetworkDiscovery,
    transports: Transports)(implicit enc: NioEncoder[Frame[Message]], dec: NioDecoder[Frame[Message]]) {

  private val conversationalNetwork =
    new ConversationalNetwork[Message](networkDiscovery, transports)

  /**
    * Disseminate a message to those on the network who want it.
    *
    * @param message the message to send.
    */
  def disseminateMessage(message: Message): Unit =
    networkDiscovery
      .nearestNPeersTo(transports.peerInfo.nodeId, Int.MaxValue)
      .foreach(peer => {
        conversationalNetwork.sendMessage(peer.nodeId, message)
      })

}
