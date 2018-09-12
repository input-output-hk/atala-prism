package io.iohk.cef.network

import java.net.InetSocketAddress

import io.iohk.cef.network.discovery.NetworkDiscovery
import io.iohk.cef.network.encoding.nio._
import io.iohk.cef.network.monixstream.MonixMessageStream
import io.iohk.cef.network.transport.Transports.usesTcp
import io.iohk.cef.network.transport._

import monix.reactive.Observable

/**
  * Represents a conversational model of the network
  * where peers exchange messages in a point to point
  * fashion.
  *
  * The term 'conversational' is used in the sense introduced
  * by Van Jacobson, to distinguish from the 'disseminational'
  * networking style.
  *
  * @param networkDiscovery Encapsulates a routing table implementation.
  * @param transports helpers to obtain network transport instances.
  */
class ConversationalNetwork[Message: NioEncoder: NioDecoder](
    networkDiscovery: NetworkDiscovery,
    transports: Transports) {

  val peerInfo: PeerInfo = transports.peerInfo

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

  def messageStream: MessageStream[Message] =
    if (usesTcp(peerInfo))
      new MonixMessageStream(
        tcpNetworkTransport.get.monixMessageStream.filter(frameHandler).map((frame: Frame[Message]) => frame.content))
    else
      new MonixMessageStream[Message](Observable.empty)

  private def frameHandler(frame: Frame[Message]): Boolean = {
    if (thisNodeIsTheDest(frame)) {
      true
    } else { // the frame is for another node
      if (frame.header.ttl > 0) {
        // decrement the ttl and resend it.
        sendMessage(frame.copy(header = FrameHeader(frame.header.src, frame.header.dst, frame.header.ttl - 1)))
        false
      } else {
        // else the ttl is zero, so discard the message
        false
      }
    }
  }

  private def thisNodeIsTheDest(frame: Frame[Message]): Boolean =
    frame.header.dst == peerInfo.nodeId

  private val frameCodec = new NioCodec[Frame[Message]](NioEncoder[Frame[Message]], NioDecoder[Frame[Message]])

  private val tcpNetworkTransport: Option[NetworkTransport[InetSocketAddress, Frame[Message]]] =
    transports.tcp(frameCodec)

  private def sendMessage(frame: Frame[Message]): Unit = {
    networkDiscovery
      .nearestPeerTo(frame.header.dst)
      .foreach(remotePeerInfo => {
        if (usesTcp(peerInfo) && usesTcp(remotePeerInfo))
          tcpNetworkTransport.get
            .sendMessage(remotePeerInfo.configuration.tcpTransportConfiguration.get.natAddress, frame)
        else
          ()
      })
  }
}
