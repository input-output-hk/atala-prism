package io.iohk.cef.network
import java.nio.ByteBuffer

import io.iohk.cef.network.discovery.NetworkDiscovery
import io.iohk.cef.network.encoding._
import io.iohk.cef.network.transport.tcp.{NettyTransport, TcpNetworkTransport}

import io.iohk.cef.network.transport.{FrameHeader, Frame, FrameEncoder, FrameDecoder}

/**
  * Represents a conversational model of the network
  * where peers exchange messages in a point to point
  * fashion.
  *
  * The term 'conversational' is used in the sense introduced
  * by Van Jacobson, to distinguish from the 'disseminational'
  * networking style.
  */
class ConversationalNetwork[Message](peerInfo: PeerInfo,
                                     /**
                                       * The messageHandler receives inbound messages from other network addresses.
                                       */
                                     messageHandler: (NodeId, Message) => Unit,
                                     messageCodec: Codec[Message, ByteBuffer],
                                     networkDiscovery: NetworkDiscovery) {

  /**
    * This describes the number of hops before discarding the message.
    */
  val messageTtl = 5

  private def frameHandler(frame: Frame[Message]): Unit = {
    if (frame.header.dst == peerInfo.nodeId) {
      messageHandler(frame.header.src, frame.content)
    } else {
      // else if the ttl is zero, discard the message
      // else decrement the message ttl and resend it.
      ???
    }
  }

  private def liftedFrameHandler[Address]: (Address, Frame[Message]) => Unit =
    (_, frame) => frameHandler(frame)

  /**
    * Message forwarding.
    * When receiving wrapped messages that are not for this node
    * we should examine the routing table to see if any peer matches
    * the message destination. If not, forward the message to 'suitable'
    * peers in the routing table.
    */
  val tcpNetworkTransport: Option[TcpNetworkTransport[Frame[Message]]] =
    peerInfo.configuration.tcpTransportConfiguration.map(tcpConfiguration => {

      val frameCodec =
        new StreamCodec[Frame[Message], ByteBuffer](new FrameEncoder[Message](messageCodec.encoder),
                                                    new FrameDecoder[Message](messageCodec.decoder))

      new TcpNetworkTransport[Frame[Message]](liftedFrameHandler,
                                              frameCodec,
                                              new NettyTransport(tcpConfiguration.bindAddress))
    })

  /**
    * Send a message to another network address.
    *
    * The process for sending a message is
    * 1. Wrap the caller's message, setting src and dst headers.
    * 2. Examine the routing table to see if an existing peer has the given address.
    *    If so, send the message directly to that peer.
    * 3. Otherwise, send the message to 'suitable' peers in the routing table.
    *
    * @param nodeId the address of the peer to which to send the message
    * @param message the message body itself.
    */
  def sendMessage(nodeId: NodeId, message: Message): Unit = {
    networkDiscovery
      .peer(nodeId)
      .foreach(remotePeerInfo => {
        val networkMessage = Frame(FrameHeader(peerInfo.nodeId, nodeId), message)
        if (usesTcp(peerInfo) && usesTcp(remotePeerInfo))
          tcpNetworkTransport.get.sendMessage(remotePeerInfo.configuration.tcpTransportConfiguration.get.natAddress,
                                              networkMessage)
        else
          ()
      })
  }

  private def usesTcp(peerInfo: PeerInfo): Boolean =
    peerInfo.configuration.tcpTransportConfiguration.isDefined
}
