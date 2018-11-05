package io.iohk.cef.ledger.identity

import java.time.Instant

import io.iohk.cef.crypto._
import io.iohk.cef.ledger.identity.storage.protobuf.identityLedger.{
  IdentityBlockProto,
  IdentityHeaderProto,
  IdentityTransactionProto
}
import io.iohk.cef.ledger.Block

import io.iohk.cef.codecs.nio._
import scala.util.Try
import java.nio.ByteBuffer
import io.iohk.cef.utils._
import scala.reflect.runtime.universe.TypeTag

object IdentityBlockSerializer {

  implicit val txSerializable: NioEncDec[IdentityTransaction] =
    new NioEncDec[IdentityTransaction] {
      override val typeTag: TypeTag[IdentityTransaction] = implicitly[TypeTag[IdentityTransaction]]

      override def encode(t: IdentityTransaction): ByteBuffer =
        encodeIdentityTransaction(t).toByteBuffer
      override def decode(u: ByteBuffer): Option[IdentityTransaction] =
        Try(IdentityTransactionProto.parseFrom(u.toArray)).toOption.flatMap(decodeIdentityTransactionProto)
    }

  implicit val serializable: NioEncDec[IdentityLedgerBlock] = {
    new NioEncDec[IdentityLedgerBlock] {
      override val typeTag: TypeTag[IdentityLedgerBlock] = implicitly[TypeTag[IdentityLedgerBlock]]

      override def decode(bytes: ByteBuffer): Option[IdentityLedgerBlock] = {
        for {
          proto <- Try(IdentityBlockProto.parseFrom(bytes.toArray)).toOption
        } yield {
          val transactions = proto.transactions.flatMap(decodeIdentityTransactionProto).toList
          Block(
            IdentityBlockHeader(Instant.ofEpochMilli(proto.header.createdEpochMilli)),
            transactions
          )
        }
      }
      override def encode(t: IdentityLedgerBlock): ByteBuffer = {
        val proto = IdentityBlockProto(
          IdentityHeaderProto(t.header.created.toEpochMilli),
          t.transactions.map(encodeIdentityTransaction)
        )
        proto.toByteBuffer
      }
    }
  }

  private val ClaimTxType = 1
  private val LinkTxType = 2
  private val UnlinkTxType = 3

  private def encodeIdentityTransaction(tx: IdentityTransaction): IdentityTransactionProto = {
    val signature = com.google.protobuf.ByteString.copyFrom(tx.signature.toByteString.toArray)

    val txType = tx match {
      case _: Claim => ClaimTxType
      case _: Link => LinkTxType
      case _: Unlink => UnlinkTxType
    }

    IdentityTransactionProto(
      txType,
      tx.identity,
      com.google.protobuf.ByteString.copyFrom(tx.key.toByteString.toArray),
      signature)
  }

  private def decodeIdentityTransactionProto(ptx: IdentityTransactionProto): Option[IdentityTransaction] = {
    // TODO: what do we do with the error handling?
    val result = for {
      key <- SigningPublicKey
        .decodeFrom(ptx.publicKey.toByteString)
        .left
        .map { error =>
          throw new RuntimeException(s"Unable to parse transaction public key: $error")
        }

      signature <- Signature
        .decodeFrom(ptx.signature.toByteString)
        .left
        .map { error =>
          throw new RuntimeException(s"Unable to decode transaction signature: $error")
        }

    } yield
      ptx.`type` match {
        case ClaimTxType => Claim(ptx.identity, key, signature)
        case LinkTxType => Link(ptx.identity, key, signature)
        case UnlinkTxType => Unlink(ptx.identity, key, signature)
      }

    // safe way to avoid result.right.get, all values here are of type Right
    result.toOption
  }
}
