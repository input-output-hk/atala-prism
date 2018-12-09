package io.iohk.cef.network

import io.iohk.cef.network.discovery.NetworkDiscovery
import io.iohk.cef.codecs.nio.NioCodec
import io.iohk.cef.network.transport.Transports
import io.iohk.cef.network.transport.Frame
import scala.reflect.runtime.universe._

class DisseminationalNetwork[Message: TypeTag](networkDiscovery: NetworkDiscovery, transports: Transports)(
    implicit codec: NioCodec[Frame[Message]]) {

  private val conversationalNetwork =
    new ConversationalNetwork[Message](networkDiscovery, transports)

  /**
    * Disseminate a message to those on the network who want it.
    *
    * @param message the message to send.
    */
  def disseminateMessage(message: Message): Unit =
    networkDiscovery
      .nearestNPeersTo(transports.peerConfig.nodeId, Int.MaxValue)
      .foreach(peer => {
        conversationalNetwork.sendMessage(peer.nodeId, message)
      })

}
