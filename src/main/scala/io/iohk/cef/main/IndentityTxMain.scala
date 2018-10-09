package io.iohk.cef.main

import java.util.Base64

import akka.http.scaladsl.Http
import akka.util.{ByteString, Timeout}
import io.iohk.cef.core.NodeCore
import io.iohk.cef.crypto._
import io.iohk.cef.ledger.identity.storage.protobuf.IdentityLedgerState.PublicKeyListProto
import io.iohk.cef.ledger.identity.{IdentityBlockHeader, IdentityBlockSerializer, IdentityTransaction}
import io.iohk.cef.ledger.{Block, ByteStringSerializable}
import io.iohk.cef.main.builder._
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
    with LedgerBuilder[S, T] with Logger with NetworkBuilder[S, H, T] with DefaultLedgerStateStorageBuilder[S]
    with LedgerStorageBuilder with DefaultActorSystemBuilder with CommonTypeAliases[S, H, T]

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
