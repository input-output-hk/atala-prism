package io.iohk.cef.demo

import java.net.{InetSocketAddress, URI}

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.io.Tcp.{Bind, Bound, CommandFailed, Connected}
import akka.io.{IO, Tcp}
import io.iohk.cef.network.NodeInfo
import io.iohk.cef.network.transport.rlpx.RLPxConnectionHandler.{ConnectTo, HandleConnection}
import SimpleNode.StartServer

class SimpleNode(val nodeId: String, protoHandler: ActorRef, bootstrapPeer: Option[NodeInfo]) extends Actor with ActorLogging {

  import context.system

  override def receive: Receive = {
    case StartServer(address) =>
      IO(Tcp) ! Bind(self, address)
      context become waitingForBindingResult
  }

  def waitingForBindingResult: Receive = {
    case Bound(localAddress) =>

      log.info("Bound: {}", nodeUri(nodeId, localAddress))

      bootstrapPeer.foreach(connectToPeer)

      context become listening

    case CommandFailed(b: Bind) =>
      log.warning("Binding to {} failed", b.localAddress)
      context stop self

    case m => println(s"Got unhandled message $m")
  }

  private def connectToPeer(peerInfo: NodeInfo): Unit =
    protoHandler ! ConnectTo(new URI(nodeUri(peerInfo.nodeId, peerInfo.inetSocketAddress)))

  private def nodeUri(nodeId: String, address: InetSocketAddress): String =
    s"enode://$nodeId@${address.getHostName}:${address.getPort}"

  def listening: Receive = {
//    case sm:SendMessage =>
//      protoHandler ! sm
    case Connected(_, _) =>
      val connection = sender()
      protoHandler ! HandleConnection(connection)
  }
}

object SimpleNode {
  def props(nodeId: String, protoHandler: ActorRef, bootstrapPeers: Option[NodeInfo] = None): Props =
    Props(new SimpleNode(nodeId, protoHandler, bootstrapPeers))

  case class StartServer(address: InetSocketAddress)
}
