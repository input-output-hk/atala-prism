package io.iohk.cef.test
import akka.util.ByteString
import io.iohk.cef.ledger.{Block, ByteStringSerializable}
import io.iohk.cef.protobuf.Common.BlockProto

import scala.collection.immutable
import scala.util.Try

object DummyBlockSerializable {
  implicit val serializable: ByteStringSerializable[Block[String, DummyBlockHeader, DummyTransaction]] =
    new ByteStringSerializable[Block[String, DummyBlockHeader, DummyTransaction]] {
      override def encode(t: Block[String, DummyBlockHeader, DummyTransaction]): ByteString = {
        val txs = t.transactions.map(tx =>
          com.google.protobuf.ByteString.copyFrom(DummyTransaction.serializable.encode(tx).toArray))
        val header = com.google.protobuf.ByteString.copyFrom(DummyBlockHeader.serializable.encode(t.header).toArray)
        ByteString(BlockProto(header, txs).toByteArray)
      }
      override def decode(bytes: ByteString): Option[Block[String, DummyBlockHeader, DummyTransaction]] = {
        for {
          proto <- Try(BlockProto.parseFrom(bytes.toArray)).toOption
          header <- DummyBlockHeader.serializable.decode(ByteString(proto.header.toByteArray))
          txs = immutable.Seq(proto.txs.map(tx => DummyTransaction.serializable.decode(ByteString(tx.toByteArray))): _*)
          flattenedTxs = txs.flatten
          blockTransactions <- Try(
            if (txs.size == flattenedTxs.size) flattenedTxs
            else throw new IllegalArgumentException(s"Wrong transaction format")
          ).toOption
        } yield {
          Block(header, blockTransactions)
        }
      }
    }
}
