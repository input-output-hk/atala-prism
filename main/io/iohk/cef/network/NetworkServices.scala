package io.iohk.cef.network
import java.net.InetSocketAddress
import java.security.SecureRandom
import java.time.Clock

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.ActorContext
import io.iohk.cef.codecs.nio._
import io.iohk.cef.codecs.nio.auto._
import io.iohk.cef.network.NodeStatus.NodeState
import io.iohk.cef.network.discovery.DiscoveryListener.DiscoveryListenerRequest
import io.iohk.cef.network.discovery.DiscoveryManager.DiscoveryRequest
import io.iohk.cef.network.discovery.db.DummyKnownNodeStorage
import io.iohk.cef.network.discovery._
import io.iohk.cef.network.telemetry.InMemoryTelemetry

object NetworkServices {

  def networkDiscovery(clock: Clock, peerConfig: PeerConfig, discoveryConfig: DiscoveryConfig): NetworkDiscovery =
    new DiscoveryManagerAdapter(discoveryManagerBehavior(clock, peerConfig, discoveryConfig))

  private def discoveryManagerBehavior(
      clock: Clock,
      peerConfig: PeerConfig,
      discoveryConfig: DiscoveryConfig
  ): Behavior[DiscoveryRequest] = {

    val nodeInfo = peerConfig2NodeInfoHack(peerConfig)

    val nodeState = NodeState(
      nodeInfo.id,
      ServerStatus.Listening(nodeInfo.serverAddress),
      ServerStatus.Listening(nodeInfo.discoveryAddress),
      Capabilities(0)
    )

    val codec: NioCodec[DiscoveryWireMessage] = NioCodec[DiscoveryWireMessage]

    val discoveryBehavior = DiscoveryManager.behaviour(
      discoveryConfig,
      new DummyKnownNodeStorage(clock) with InMemoryTelemetry,
      nodeState,
      clock,
      codec,
      listenerFactory(discoveryConfig, codec),
      new SecureRandom(),
      InMemoryTelemetry.registry
    )
    discoveryBehavior
  }

  private def listenerFactory(discoveryConfig: DiscoveryConfig, codec: NioCodec[DiscoveryWireMessage])(
      context: ActorContext[DiscoveryRequest]
  ): ActorRef[DiscoveryListenerRequest] = {

    context.spawn(
      DiscoveryListener.behavior(discoveryConfig, UDPBridge.creator(discoveryConfig, codec)),
      "DiscoveryListener"
    )
  }

  // FIXME Get rid of NodeInfo
  private def peerConfig2NodeInfoHack(peerConfig: PeerConfig): NodeInfo = {
    val discoveryAddress =
      new InetSocketAddress("localhost", peerConfig.networkConfig.tcpTransportConfig.get.bindAddress.getPort + 1)

    val serverAddress = peerConfig.networkConfig.tcpTransportConfig.get.natAddress

    NodeInfo(peerConfig.nodeId.id, discoveryAddress, serverAddress, Capabilities(0))
  }
}
