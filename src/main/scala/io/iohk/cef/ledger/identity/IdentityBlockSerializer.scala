package io.iohk.cef.ledger.identity

import java.time.Instant

import akka.util.ByteString
import io.iohk.cef.crypto.low.{DigitalSignature, decodePublicKey}
import io.iohk.cef.ledger.identity.storage.protobuf.identityLedger.{IdentityBlockProto, IdentityHeaderProto, IdentityTransactionProto}
import io.iohk.cef.ledger.{Block, ByteStringSerializable}

object IdentityBlockSerializer {

  implicit val serializable: ByteStringSerializable[IdentityLedgerBlock] = {
    new ByteStringSerializable[IdentityLedgerBlock] {

      val ClaimTxType = 1
      val LinkTxType = 2
      val UnlinkTxType = 3

      override def deserialize(bytes: ByteString) = {
        val proto = IdentityBlockProto.parseFrom(bytes.toArray)
        Block(
          IdentityBlockHeader(ByteString(proto.header.hash.toByteArray), Instant.ofEpochMilli(proto.header.createdEpochMilli), proto.header.blockHeight),
          proto.transactions.map(ptx => {
            val key = decodePublicKey(ptx.publicKey.toByteArray)
            val signature = new DigitalSignature(ByteString(ptx.signature.toByteArray))

            ptx.`type` match {
              case ClaimTxType => Claim(ptx.identity, key, signature)
              case LinkTxType => Link(ptx.identity, key, signature)
              case UnlinkTxType => Unlink(ptx.identity, key, signature)
            }
          }).toList
        )
      }

      override def serialize(t: IdentityLedgerBlock): ByteString = {
        val proto = IdentityBlockProto(
          IdentityHeaderProto(
            com.google.protobuf.ByteString.copyFrom(t.header.hash.toArray),
            t.header.height,
            t.header.created.toEpochMilli),
            t.transactions.map { tx =>
              val signature = com.google.protobuf.ByteString.copyFrom(tx.signature.value.toArray)

              val txType = tx match {
                case _: Claim => ClaimTxType
                case _: Link => LinkTxType
                case _: Unlink => UnlinkTxType
              }

              IdentityTransactionProto(
                txType,
                tx.identity,
                com.google.protobuf.ByteString.copyFrom(tx.key.getEncoded),
                signature)
            }
        )
        ByteString(proto.toByteArray)
      }
    }
  }
}
