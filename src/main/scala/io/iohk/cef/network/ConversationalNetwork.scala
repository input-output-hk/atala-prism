package io.iohk.cef.network

import io.iohk.cef.network.discovery.NetworkDiscovery
import io.iohk.cef.network.encoding.nio._
import io.iohk.cef.network.transport.Transports.usesTcp
import io.iohk.cef.network.transport.tcp.TcpNetworkTransport
import io.iohk.cef.network.transport._

/**
  * Represents a conversational model of the network
  * where peers exchange messages in a point to point
  * fashion.
  *
  * The term 'conversational' is used in the sense introduced
  * by Van Jacobson, to distinguish from the 'disseminational'
  * networking style.
  *
  * @param peerInfo the peer configuration of this node
  * @param messageHandler The messageHandler receives inbound messages from remote peers.
  * @param networkDiscovery Encapsulates a routing table implementation.
  * @param transports helpers to obtain network transport instances.
  */
class ConversationalNetwork[Message: NioEncoder: NioDecoder: Default](peerInfo: PeerInfo,
                                                                      messageHandler: (NodeId, Message) => Unit,
                                                                      networkDiscovery: NetworkDiscovery,
                                                                      transports: Transports) {

  /**
    * Send a message to another network address.
    *
    * The process for sending a message is
    * 1. Wrap the caller's message, setting src and dst headers.
    * 2. Examine the routing table to find a suitable peer to which to send the messagesee if an existing peer has the given address.
    *    If so, send the message directly to that peer.
    * 3. Otherwise, send the message to 'suitable' peers in the routing table.
    *
    * @param nodeId the address of the peer to which to send the message
    * @param message the message body itself.
    */
  def sendMessage(nodeId: NodeId, message: Message): Unit =
    sendMessage(Frame(FrameHeader(peerInfo.nodeId, nodeId, peerInfo.configuration.messageTtl), message))

  private def frameHandler(frame: Frame[Message]): Unit = {
    if (thisNodeIsTheDest(frame)) {
      messageHandler(frame.header.src, frame.content)
    } else { // the frame is for another node
      if (frame.header.ttl > 0) {
        // decrement the ttl and resend it.
        sendMessage(frame.copy(header = FrameHeader(frame.header.src, frame.header.dst, frame.header.ttl - 1)))
      }
      // else the ttl is zero, so discard the message
    }
  }

  private def thisNodeIsTheDest(frame: Frame[Message]): Boolean =
    frame.header.dst == peerInfo.nodeId

  private def liftedFrameHandler[Address]: (Address, Frame[Message]) => Unit =
    (_, frame) => frameHandler(frame)

  private val frameCodec = new NioStreamCodec[Frame[Message]](NioEncoder[Frame[Message]], NioDecoder[Frame[Message]])

  private val tcpNetworkTransport: Option[TcpNetworkTransport[Frame[Message]]] =
    transports.tcp(liftedFrameHandler)(frameCodec)

  private def sendMessage(frame: Frame[Message]): Unit = {
    networkDiscovery
      .peer(frame.header.dst)
      .foreach(remotePeerInfo => {
        if (usesTcp(peerInfo) && usesTcp(remotePeerInfo))
          tcpNetworkTransport.get.sendMessage(remotePeerInfo.configuration.tcpTransportConfiguration.get.natAddress,
                                              frame)
        else
          ()
      })
  }
}
