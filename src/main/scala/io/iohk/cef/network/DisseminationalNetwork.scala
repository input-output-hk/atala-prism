package io.iohk.cef.network
import io.iohk.cef.network.discovery.NetworkDiscovery
import io.iohk.cef.network.encoding.nio.{NioDecoder, NioEncoder}
import io.iohk.cef.network.transport.Transports
import scala.reflect.runtime.universe._

class DisseminationalNetwork[Message: NioEncoder: NioDecoder: WeakTypeTag](
    networkDiscovery: NetworkDiscovery,
    transports: Transports) {

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
