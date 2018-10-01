package io.iohk.cef.ledger.identity

import java.time.Instant

import akka.util.ByteString
import io.iohk.cef.crypto._
import io.iohk.cef.ledger.identity.storage.protobuf.identityLedger.{
  IdentityBlockProto,
  IdentityHeaderProto,
  IdentityTransactionProto
}
import io.iohk.cef.ledger.{Block, ByteStringSerializable}

import scala.util.Try

object IdentityBlockSerializer {

  implicit val txSerializable: ByteStringSerializable[IdentityTransaction] =
    new ByteStringSerializable[IdentityTransaction] {

      override def encode(t: IdentityTransaction): ByteString =
        ByteString(encodeIdentityTransaction(t).toByteArray)
      override def decode(u: ByteString): Option[IdentityTransaction] =
        decodeIdentityTransactionProto(IdentityTransactionProto.parseFrom(u.toArray))
    }

  implicit val serializable: ByteStringSerializable[IdentityLedgerBlock] = {
    new ByteStringSerializable[IdentityLedgerBlock] {

      override def decode(bytes: ByteString): Option[IdentityLedgerBlock] = {
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
      override def encode(t: IdentityLedgerBlock): ByteString = {
        val proto = IdentityBlockProto(
          IdentityHeaderProto(t.header.created.toEpochMilli),
          t.transactions.map(encodeIdentityTransaction)
        )
        ByteString(proto.toByteArray)
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
        .decodeFrom(ByteString(ptx.publicKey.toByteArray))
        .left
        .map { error =>
          throw new RuntimeException(s"Unable to parse transaction public key: $error")
        }

      signature <- Signature
        .decodeFrom(ByteString(ptx.signature.toByteArray))
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
