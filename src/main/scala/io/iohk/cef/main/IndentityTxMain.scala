package io.iohk.cef.main

import java.net.InetSocketAddress
import java.security.SecureRandom
import java.time.Clock

import akka.actor.typed.ActorRef
import akka.actor.typed.scaladsl.ActorContext
import akka.util.{ByteString, Timeout}
import io.iohk.cef.LedgerId
import io.iohk.cef.consensus.raft
import io.iohk.cef.consensus.raft.LogEntry
import io.iohk.cef.consensus.raft.node.OnDiskPersistentStorage
import io.iohk.cef.core.{Envelope, NodeCore}
import io.iohk.cef.core.raftrpc.RaftRPCFactory
import io.iohk.cef.crypto._
import io.iohk.cef.ledger.{Block, ByteStringSerializable}
import io.iohk.cef.ledger.identity.{IdentityBlockHeader, IdentityTransaction}
import io.iohk.cef.main.builder.base._
import io.iohk.cef.main.builder.derived._
import io.iohk.cef.main.builder.helpers.LedgerConfig
import io.iohk.cef.network.NodeStatus.NodeState
import io.iohk.cef.network._
import io.iohk.cef.network.discovery.DiscoveryListener.DiscoveryListenerRequest
import io.iohk.cef.network.discovery.DiscoveryManager.DiscoveryRequest
import io.iohk.cef.network.discovery._
import io.iohk.cef.network.discovery.db.DummyKnownNodesStorage
import io.iohk.cef.network.encoding.nio.NioCodecs._
import io.iohk.cef.network.encoding.{Decoder, Encoder}
import io.iohk.cef.network.telemetry.InMemoryTelemetry
import io.iohk.cef.network.transport.Transports
import io.iohk.cef.network.transport.tcp.TcpTransportConfiguration

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._

object IndentityTxMain extends App {

  trait DefaultLedgerConfig extends LedgerConfigBuilder {
    override val clock: Clock = Clock.systemUTC()
    override val ledgerConfig: LedgerConfig = new LedgerConfig(
      10000,
      1 minute,
      1 minute,
      1 minute
    )
    val interface = "localhost"
    override val ledgerId: LedgerId = 1

    override val serverAddress = InetSocketAddress.createUnresolved(interface, 9001)
    override val discoveryAddress = InetSocketAddress.createUnresolved(interface, 9002)
    override val nodeIdStr = "1234"
    override val nodeId: NodeId = NodeId(nodeIdStr)
    override val bootstrapPeers: Set[PeerInfo] = Set()
    val networkConfiguration = NetworkConfiguration(Some(TcpTransportConfiguration(serverAddress)))
    val peerInfo = PeerInfo(NodeId(nodeIdStr), networkConfiguration)
    val capabilities = Capabilities(1)
    val nodeInfo = NodeInfo(peerInfo.nodeId.id, discoveryAddress, serverAddress, capabilities)
    val nodeState = NodeState(
      nodeInfo.id,
      ServerStatus.Listening(nodeInfo.serverAddress),
      ServerStatus.Listening(nodeInfo.discoveryAddress),
      capabilities)

    override val discoveryConfig: DiscoveryConfig = DiscoveryConfig(
      discoveryEnabled = true,
      interface = interface,
      port = discoveryAddress.getPort,
      bootstrapNodes = Set(),
      discoveredNodesLimit = 100,
      scanNodesLimit = 100,
      concurrencyDegree = 100,
      scanInitialDelay = 0 millis,
      scanInterval = 1 minute,
      messageExpiration = 1 minute,
      maxSeekResults = 100,
      multipleConnectionsPerAddress = true,
      blacklistDefaultDuration = 1 minute
    )

    private def discoveryBehavior(implicit discoveryMsgSerializer: ByteStringSerializable[DiscoveryWireMessage]) =
      DiscoveryManager.behaviour(
        discoveryConfig,
        new DummyKnownNodesStorage(clock) with InMemoryTelemetry,
        nodeState,
        clock,
        discoveryMsgSerializer,
        discoveryMsgSerializer,
        listenerFactory(discoveryConfig, discoveryMsgSerializer, discoveryMsgSerializer),
        new SecureRandom(),
        InMemoryTelemetry.registry
      )

    private def listenerFactory(
        discoveryConfig: DiscoveryConfig,
        encoder: Encoder[DiscoveryWireMessage, ByteString],
        decoder: Decoder[ByteString, DiscoveryWireMessage])(
        context: ActorContext[DiscoveryRequest]): ActorRef[DiscoveryListenerRequest] = {

      context.spawn(
        DiscoveryListener.behavior(discoveryConfig, UDPBridge.creator(discoveryConfig, encoder, decoder)),
        "DiscoveryListener")
    }

    override val transports: Transports = new Transports(peerInfo)
    override def networkDiscovery(
        implicit discoveryMsgSerializer: ByteStringSerializable[DiscoveryWireMessage]): NetworkDiscovery =
      new DiscoveryManagerAdapter(discoveryBehavior)
  }

  trait DefaultRaftConsensusConfigBuilder[Command] extends RaftConsensusConfigBuilder[Command] {
    self: DefaultLedgerConfig =>
    import io.iohk.cef.network.encoding.array.ArrayCodecs._
    override def storage(
        implicit logEntrySerializable: ByteStringSerializable[LogEntry[Command]],
        commandSerializable: ByteStringSerializable[Command]): raft.PersistentStorage[Command] = {
      new OnDiskPersistentStorage[Command](nodeIdStr)
    }
    override val clusterMemberIds: Seq[String] = Seq()
    override val electionTimeoutRange: (Duration, Duration) = (10 minutes, 10 minutes)
    override val heartbeatTimeoutRange: (Duration, Duration) = (5 minutes, 5 minutes)
    override def rpcFactory(
        implicit serializable: ByteStringSerializable[DiscoveryWireMessage],
        commandSerializable: ByteStringSerializable[Command],
        executionContext: ExecutionContext): raft.RPCFactory[Command] = {
      implicit val commandNioEncoder = commandSerializable.toNioEncoder
      implicit val commandNioDecoder = commandSerializable.toNioDecoder
      new RaftRPCFactory[Command](networkDiscovery, transports)
    }
  }
  type S = Set[SigningPublicKey]
  type H = IdentityBlockHeader
  type T = IdentityTransaction
  type B = Block[S, H, T]
  type ET = Envelope[T]
  type EB = Envelope[B]

  def identityCoreBuilder(
      implicit
      txNetworkEncoder: NioEncoder[ET],
      txNetworkDecoder: NioDecoder[ET],
      blockNetworkEncoder: NioEncoder[EB],
      blockNetworkDecoder: NioDecoder[EB],
      timeout: Timeout,
      executionContext: ExecutionContext,
      blockByteStringSerializable: ByteStringSerializable[B],
      stateyteStringSerializable: ByteStringSerializable[S],
      ebByteStringSerializable: ByteStringSerializable[EB],
      etStringSerializable: ByteStringSerializable[ET],
      discoverMsgEncoder: Encoder[DiscoveryWireMessage, ByteString],
      dByteStringSerializable: ByteStringSerializable[DiscoveryWireMessage],
      lByteStringSerializable: ByteStringSerializable[LogEntry[B]]): NodeCore[S, H, T] = {
    val coreBuilder = new NodeCoreBuilder[S, H, T] with DefaultLedgerConfig with DefaultRaftConsensusConfigBuilder[B]
    with RaftConsensusBuilder[S, H, T] with TransactionPoolBuilder[S, H, T] with LedgerBuilder[Future, S, T]
    with LogBuilder with NetworkBuilder[S, H, T] with DefaultLedgerStateStorageBuilder[S] with LedgerStorageBuilder
    with DefaultActorSystemBuilder with CommonTypeAliases[S, H, T]
    //Identity Specific
    with IdentityLedgerHeaderGenerator {}
    coreBuilder.nodeCore
  }
}
