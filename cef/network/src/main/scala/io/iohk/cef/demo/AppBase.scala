package io.iohk.cef.demo

import java.net.InetSocketAddress
import java.security.SecureRandom
import java.time.Clock

import akka.actor.typed.ActorRef
import akka.actor.typed.scaladsl.adapter._
import akka.util.ByteString
import akka.{actor => untyped}
import com.typesafe.config.ConfigFactory
import io.iohk.cef.db.KnownNodeStorageImpl
import io.iohk.cef.discovery.DiscoveryManager.DiscoveryRequest
import io.iohk.cef.discovery._
import io.iohk.cef.encoding.{Decoder, Encoder}
import io.iohk.cef.network.NodeStatus.NodeState
import io.iohk.cef.network.{Capabilities, Node, ServerStatus}
import io.iohk.cef.utils.Logger
import io.micrometer.core.instrument.simple.SimpleMeterRegistry

trait AppBase extends Logger {

  val configFile = ConfigFactory.load()

  val discoveryConfig = DiscoveryConfig.fromConfig(configFile)

  import io.iohk.cef.encoding.rlp.RLPEncoders._
  import io.iohk.cef.encoding.rlp.RLPImplicits._

  val encoder = implicitly[Encoder[DiscoveryWireMessage, ByteString]]

  val decoder = implicitly[Decoder[ByteString, DiscoveryWireMessage]]

  def createActor(id: Int, bootstrapNodeIds: Set[Int], capabilities: Capabilities)
                 (implicit system: untyped.ActorSystem):
  ActorRef[DiscoveryRequest] = {

    val state = new NodeState(ByteString(id), ServerStatus.NotListening, ServerStatus.NotListening, capabilities)
    val portBase = discoveryConfig.port
    val address = discoveryConfig.interface
    val bootstrapNodes = bootstrapNodeIds.map(nodeId =>
      Node(ByteString(nodeId.toString),
        new InetSocketAddress(address, portBase + nodeId),
        new InetSocketAddress(address, portBase + nodeId), state.capabilities)
    )

    val config = discoveryConfig.copy(port = portBase + id, bootstrapNodes = bootstrapNodes)
    val secureRandom = new SecureRandom()

    val behavior = DiscoveryManager.behaviour(
      config,
      new KnownNodeStorageImpl(Clock.systemUTC()),
      state,
      Clock.systemUTC(),
      encoder,
      decoder,
      context => context.spawn(
        DiscoveryListener.behavior(config,
          UDPBridge.creator(config, encoder, decoder)), "DiscoveryListener"),
      secureRandom,
      new SimpleMeterRegistry()
    )

    system.spawn(behavior, "discovery_behaviour")
  }
}
