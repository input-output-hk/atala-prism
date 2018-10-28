package io.iohk.cef.main.builder

import java.security.SecureRandom
import java.time.Clock

import akka.actor.typed.ActorRef
import akka.actor.typed.scaladsl.ActorContext
import akka.util.ByteString
import io.iohk.cef.ledger.{ByteStringSerializable, LedgerConfig}
import io.iohk.cef.network.NodeStatus.NodeState
import io.iohk.cef.network._
import io.iohk.cef.network.discovery.DiscoveryListener.DiscoveryListenerRequest
import io.iohk.cef.network.discovery.DiscoveryManager.DiscoveryRequest
import io.iohk.cef.network.discovery._
import io.iohk.cef.network.discovery.db.DummyKnownNodesStorage
import io.iohk.cef.network.encoding.{Decoder, Encoder}
import io.iohk.cef.network.telemetry.InMemoryTelemetry
import io.iohk.cef.network.transport.Transports
import io.iohk.cef.network.transport.tcp.TcpTransportConfiguration
import org.bouncycastle.util.encoders.Hex

trait LedgerConfigBuilder {
  val clock: Clock
  val nodeId: NodeId

  val ledgerConfig: LedgerConfig
  //Network
  val networkConfig: NetworkConfig
  val nodeIdStr: String
  val transports: Transports
  val networkDiscovery: NetworkDiscovery
}

class DefaultLedgerConfig(configReaderBuilder: ConfigReaderBuilder) extends LedgerConfigBuilder {

  import configReaderBuilder._

  import io.iohk.cef.network.encoding.rlp.RLPImplicits._
  import io.iohk.cef.network.encoding.rlp._
  val discoveryMsgSerializer = implicitly[ByteStringSerializable[DiscoveryWireMessage]]
  override val clock: Clock = Clock.systemUTC()
  override val ledgerConfig: LedgerConfig = LedgerConfig(config)

  override val networkConfig = NetworkConfig(config.getConfig("network"))
  override val nodeIdStr = config.getString("node.id")
  override val nodeId: NodeId = NodeId(nodeIdStr)
  val networkConfiguration = NetworkConfiguration(Some(TcpTransportConfiguration(networkConfig.serverConfig.address)))
  val peerInfo = PeerInfo(NodeId(nodeIdStr), networkConfiguration)
  val capabilities = Capabilities(Hex.decode(config.getString("node.capabilities")).head)
  val nodeInfo = NodeInfo(
    peerInfo.nodeId.id,
    networkConfig.discoveryConfig.discoveryAddress,
    networkConfig.serverConfig.address,
    capabilities)
  val nodeState = NodeState(
    nodeInfo.id,
    ServerStatus.Listening(nodeInfo.serverAddress),
    ServerStatus.Listening(nodeInfo.discoveryAddress),
    capabilities)

  private val discoveryBehavior =
    DiscoveryManager.behaviour(
      networkConfig.discoveryConfig,
      new DummyKnownNodesStorage(clock) with InMemoryTelemetry,
      nodeState,
      clock,
      discoveryMsgSerializer,
      discoveryMsgSerializer,
      listenerFactory(discoveryMsgSerializer, discoveryMsgSerializer),
      new SecureRandom(),
      InMemoryTelemetry.registry
    )

  private def listenerFactory(
      encoder: Encoder[DiscoveryWireMessage, ByteString],
      decoder: Decoder[ByteString, DiscoveryWireMessage])(
      context: ActorContext[DiscoveryRequest]): ActorRef[DiscoveryListenerRequest] = {

    context.spawn(
      DiscoveryListener
        .behavior(networkConfig.discoveryConfig, UDPBridge.creator(networkConfig.discoveryConfig, encoder, decoder)),
      "DiscoveryListener")
  }

  override val transports: Transports = new Transports(peerInfo)
  override val networkDiscovery: NetworkDiscovery =
    new DiscoveryManagerAdapter(discoveryBehavior)
}
