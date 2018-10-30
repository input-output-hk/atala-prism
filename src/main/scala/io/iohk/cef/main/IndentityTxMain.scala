package io.iohk.cef.main

import java.util.Base64

import akka.http.scaladsl.Http
import akka.util.{ByteString, Timeout}
import io.iohk.cef.codecs.array.ArrayCodecs._
import io.iohk.cef.consensus.raft.LogEntry
import io.iohk.cef.core.NodeCore
import io.iohk.cef.crypto._
import io.iohk.cef.frontend.models.IdentityTransactionType
import io.iohk.cef.ledger.identity.storage.protobuf.IdentityLedgerState.PublicKeyListProto
import io.iohk.cef.ledger.identity.{IdentityBlockHeader, IdentityBlockSerializer, IdentityTransaction}
import io.iohk.cef.ledger.{Block, ByteStringSerializable}
import io.iohk.cef.main.builder.{IdentityFrontendBuilder, _}
import io.iohk.cef.network.discovery._
import io.iohk.cef.network.encoding.rlp
import io.iohk.cef.network.encoding.rlp.RLPEncDec
import io.iohk.cef.utils.Logger

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.io.StdIn

object IndentityTxMain extends App {

  type S = Set[SigningPublicKey]
  type H = IdentityBlockHeader
  type T = IdentityTransaction
  type B = Block[S, H, T]

  def identityCoreBuilder(coreBuilder: NodeCoreBuilder[S, H, T], frontendBuilder: IdentityFrontendBuilder)(
      implicit
      timeout: Timeout,
      executionContext: ExecutionContext,
      blockByteStringSerializable: ByteStringSerializable[B],
      stateyteStringSerializable: ByteStringSerializable[S],
      txStringSerializable: ByteStringSerializable[T],
      dByteStringSerializable: ByteStringSerializable[DiscoveryWireMessage],
      arrayEncoder: ArrayEncoder[B],
      arrayDecoder: ArrayDecoder[B],
      arrayLEncoder: ArrayEncoder[LogEntry[B]],
      arrayLDecoder: ArrayDecoder[LogEntry[B]]): (Future[Http.ServerBinding], NodeCore[S, H, T]) = {

    val bindingF = frontendBuilder.bindingFuture
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
  val actorSystemBuilder = new DefaultActorSystemBuilder
  val logger = new Logger {}
  val ledgerStateStorageBuilder = new IdentityLedgerStateStorageBuilder
  val commonTypeAliases = new CommonTypeAliases[S, H, T]
  val configReaderBuilder = new ConfigReaderBuilder
  val ledgerConfigBuilder = new DefaultLedgerConfig(configReaderBuilder)
  val raftConsensusConfigBuilder =
    new DefaultRaftConsensusConfigBuilder[S, H, T](ledgerConfigBuilder, configReaderBuilder)
  val networkBuilder = new NetworkBuilder[S, H, T](ledgerConfigBuilder)
  val ledgerStorageBuilder = new LedgerStorageBuilder(ledgerConfigBuilder)
  val headerGeneratorBuilder = new IdentityLedgerHeaderGenerator(ledgerConfigBuilder)
  val transactionPoolBuilder = new TransactionPoolBuilder[S, H, T](
    headerGeneratorBuilder,
    ledgerStateStorageBuilder,
    ledgerConfigBuilder)
  val ledgerBuilder = new LedgerBuilder[S, T](ledgerStateStorageBuilder, ledgerStorageBuilder)
  val consensusBuilder = new RaftConsensusBuilder[S, H, T](
    ledgerConfigBuilder,
    transactionPoolBuilder,
    raftConsensusConfigBuilder,
    ledgerBuilder,
    logger,
    commonTypeAliases)
  val blockCreatorBuilder =
    new BlockCreatorBuilder[S, H, T](consensusBuilder, transactionPoolBuilder, ledgerConfigBuilder, commonTypeAliases)
  val nodeCoreBuilder = new NodeCoreBuilder[S, H, T](
    networkBuilder,
    ledgerConfigBuilder,
    transactionPoolBuilder,
    consensusBuilder,
    commonTypeAliases)
  val identityTransactionServiceBuilder = new IdentityTransactionServiceBuilder(nodeCoreBuilder, commonTypeAliases)
  val frontendBuilder = new IdentityFrontendBuilder(
    actorSystemBuilder,
    identityTransactionServiceBuilder,
    commonTypeAliases,
    configReaderBuilder)

  import io.iohk.cef.codecs.array.ArrayCodecs._

  val (serverBinding, core) = identityCoreBuilder(nodeCoreBuilder, frontendBuilder)
  StdIn.readLine() // let it run until user presses return
  Await.result(serverBinding.flatMap(_.unbind()), 1 minute) // trigger unbinding from the port

  val pair = generateSigningKeyPair()
  val ed = Base64.getEncoder
  val encodedKey = ed.encodeToString(pair.public.toByteString.toArray)
  println(encodedKey)
  val signature = IdentityTransaction.sign("carlos", IdentityTransactionType.Claim, pair.public, pair.`private`)
  println(ed.encodeToString(signature.toByteString.toArray))
}
