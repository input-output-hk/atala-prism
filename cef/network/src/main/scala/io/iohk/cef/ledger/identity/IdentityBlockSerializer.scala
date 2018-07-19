package io.iohk.cef.ledger.identity

import java.time.Instant

import akka.util.ByteString
import io.iohk.cef.ledger.{Block, ByteStringSerializable, LedgerState}
import io.iohk.cef.ledger.identity.storage.protobuf.IdentityLedger.{IdentityBlockProto, IdentityHeaderProto, IdentityTransactionProto}

object IdentityBlockSerializer {

  type IdentityLedgerBlock = Block[LedgerState[String, Set[ByteString]], String, IdentityBlockHeader, IdentityTransaction]

  implicit val serializable: ByteStringSerializable[IdentityLedgerBlock] = {
    new ByteStringSerializable[IdentityLedgerBlock] {
      override def deserialize(bytes: ByteString) = {
        val proto = IdentityBlockProto.parseFrom(bytes.toArray)
        Block(
          IdentityBlockHeader(ByteString(proto.header.hash.toByteArray), Instant.ofEpochMilli(proto.header.createdEpochMilli), proto.header.blockHeight),
          proto.transactions.map(ptx => {
            IdentityTransaction(ptx.`type`, ptx.identity, ByteString(ptx.publicKey.toByteArray))
          }).toList
        )
      }

      override def serialize(t: IdentityLedgerBlock): ByteString = {
        val proto = IdentityBlockProto(
          IdentityHeaderProto(
            com.google.protobuf.ByteString.copyFrom(t.header.hash.toArray),
            t.header.height,
            t.header.created.toEpochMilli),
            t.transactions.map(tx => {
              IdentityTransactionProto(tx.TxType, tx.identity, com.google.protobuf.ByteString.copyFrom(tx.key.toArray))
            })
        )
        ByteString(proto.toByteArray)
      }
    }
  }
}
