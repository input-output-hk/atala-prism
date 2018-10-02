package io.iohk.cef.main

import java.security.SecureRandom
import java.time.Clock
import java.util.Base64

import akka.actor.typed.ActorRef
import akka.actor.typed.scaladsl.ActorContext
import akka.http.scaladsl.Http
import akka.util.{ByteString, Timeout}
import io.iohk.cef.consensus.raft
import io.iohk.cef.consensus.raft.RaftConfig
import io.iohk.cef.consensus.raft.node.OnDiskPersistentStorage
import io.iohk.cef.core.NodeCore
import io.iohk.cef.core.raftrpc.RaftRPCFactory
import io.iohk.cef.crypto._
import io.iohk.cef.ledger.identity.storage.protobuf.IdentityLedgerState.PublicKeyListProto
import io.iohk.cef.ledger.identity.{IdentityBlockHeader, IdentityBlockSerializer, IdentityTransaction}
import io.iohk.cef.ledger.{Block, ByteStringSerializable}
import io.iohk.cef.main.builder.base.{ConfigReaderBuilder, _}
import io.iohk.cef.main.builder.derived._
import io.iohk.cef.main.builder.helpers.LedgerConfig
import io.iohk.cef.network.NodeStatus.NodeState
import io.iohk.cef.network._
import io.iohk.cef.network.discovery.DiscoveryListener.DiscoveryListenerRequest
import io.iohk.cef.network.discovery.DiscoveryManager.DiscoveryRequest
import io.iohk.cef.network.discovery._
import io.iohk.cef.network.discovery.db.DummyKnownNodesStorage
import io.iohk.cef.network.encoding.nio.NioCodecs._
import io.iohk.cef.network.encoding.rlp.RLPEncDec
import io.iohk.cef.network.encoding.{Decoder, Encoder, rlp}
import io.iohk.cef.network.telemetry.InMemoryTelemetry
import io.iohk.cef.network.transport.Transports
import io.iohk.cef.network.transport.tcp.TcpTransportConfiguration
import org.bouncycastle.util.encoders.Hex

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.io.StdIn

object IndentityTxMain extends App {

  trait DefaultLedgerConfig extends LedgerConfigBuilder {
    self: ConfigReaderBuilder =>
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

    private def discoveryBehavior(implicit discoveryMsgSerializer: ByteStringSerializable[DiscoveryWireMessage]) =
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
    override def networkDiscovery(
        implicit discoveryMsgSerializer: ByteStringSerializable[DiscoveryWireMessage]): NetworkDiscovery =
      new DiscoveryManagerAdapter(discoveryBehavior)
  }

  trait DefaultRaftConsensusConfigBuilder[Command] extends RaftConsensusConfigBuilder[Command] {
    self: DefaultLedgerConfig with ConfigReaderBuilder =>
    import io.iohk.cef.network.encoding.array.ArrayCodecs._
    override def storage(
        implicit
        commandSerializable: ByteStringSerializable[Command]): raft.PersistentStorage[Command] = {
      new OnDiskPersistentStorage[Command](nodeIdStr)
    }
    override val raftConfig: RaftConfig = RaftConfig(config.getConfig("consensus.raft"))
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

  def identityCoreBuilder(
      implicit
      timeout: Timeout,
      executionContext: ExecutionContext,
      blockByteStringSerializable: ByteStringSerializable[B],
      stateyteStringSerializable: ByteStringSerializable[S],
      txStringSerializable: ByteStringSerializable[T],
      dByteStringSerializable: ByteStringSerializable[DiscoveryWireMessage])
    : (Future[Http.ServerBinding], NodeCore[S, H, T]) = {
    val coreBuilder = new NodeCoreBuilder[S, H, T] with ConfigReaderBuilder with DefaultLedgerConfig
    with DefaultRaftConsensusConfigBuilder[B] with RaftConsensusBuilder[S, H, T] with TransactionPoolBuilder[S, H, T]
    with LedgerBuilder[Future, S, T] with LogBuilder with NetworkBuilder[S, H, T]
    with DefaultLedgerStateStorageBuilder[S] with LedgerStorageBuilder with DefaultActorSystemBuilder
    with CommonTypeAliases[S, H, T]

    //Identity Specific
    with IdentityFrontendBuilder with IdentityTransactionServiceBuilder with IdentityLedgerHeaderGenerator {}
    val bindingF = coreBuilder.bindingFuture
    (bindingF, coreBuilder.nodeCore)
  }

  //Starting up the server

  implicit val timeout: Timeout = 1 minute
  implicit val executionContext = scala.concurrent.ExecutionContext.global
  implicit val bSerializable = IdentityBlockSerializer.serializable
  implicit val txSerializable = IdentityBlockSerializer.txSerializable
  implicit val sSerializable = new ByteStringSerializable[Set[SigningPublicKey]] {
    override def decode(u: ByteString): Option[Set[SigningPublicKey]] = {
      Some(
        PublicKeyListProto
          .parseFrom(u.toArray)
          .publicKeys
          .map(bytes =>
            //TODO: error handling
            SigningPublicKey.decodeFrom(ByteString(bytes.toByteArray)).right.get)
          .toSet)
    }

    override def encode(t: Set[SigningPublicKey]): ByteString = {
      ByteString(
        PublicKeyListProto(
          t.map(b => {
              com.google.protobuf.ByteString.copyFrom(b.toByteString.toArray)
            })
            .toSeq).toByteArray)
    }
  }
  import io.iohk.cef.network.encoding.rlp.RLPImplicits._
  def discSerializable(implicit encDec: RLPEncDec[DiscoveryWireMessage]): ByteStringSerializable[DiscoveryWireMessage] =
    new ByteStringSerializable[DiscoveryWireMessage] {
      override def encode(t: DiscoveryWireMessage): ByteString = {
        ByteString(rlp.encodeToArray(t))
      }
      override def decode(u: ByteString): Option[DiscoveryWireMessage] = {
        Some(rlp.decodeFromArray[DiscoveryWireMessage](u.toArray))
      }
    }
  implicit val dSerializable = discSerializable
  implicit def nioEncoderFromByteStringSerializable[T](implicit serializable: ByteStringSerializable[T]) =
    serializable.toNioEncoder
  implicit def nioDecoderFromByteStringSerializable[T](implicit serializable: ByteStringSerializable[T]) =
    serializable.toNioDecoder
  val (serverBinding, core) = identityCoreBuilder
  StdIn.readLine() // let it run until user presses return
  Await.result(serverBinding.flatMap(_.unbind()), 1 minute) // trigger unbinding from the port

  val pair = generateSigningKeyPair()
  val ed = Base64.getEncoder
  val encodedKey = ed.encodeToString(pair.public.toByteString.toArray)
  println(encodedKey)
  val signature = IdentityTransaction.sign("carlos", pair.public, pair.`private`)
  println(ed.encodeToString(signature.toByteString.toArray))
}
