package io.iohk.cef.test
import akka.util.ByteString
import io.iohk.cef.ledger.{Block, ByteStringSerializable}
import io.iohk.cef.protobuf.Common.BlockProto

import scala.collection.immutable

object DummyBlockSerializable {
  implicit val serializable: ByteStringSerializable[Block[String, DummyBlockHeader, DummyTransaction]] =
    new ByteStringSerializable[Block[String, DummyBlockHeader, DummyTransaction]] {
      override def serialize(t: Block[String, DummyBlockHeader, DummyTransaction]): ByteString = {
        val txs = t.transactions.map(tx =>
          com.google.protobuf.ByteString.copyFrom(DummyTransaction.serializable.serialize(tx).toArray))
        val header = com.google.protobuf.ByteString.copyFrom(DummyBlockHeader.serializable.serialize(t.header).toArray)
        ByteString(BlockProto(header, txs).toByteArray)
      }
      override def deserialize(bytes: ByteString): Block[String, DummyBlockHeader, DummyTransaction] = {
        val proto = BlockProto.parseFrom(bytes.toArray)
        val header = DummyBlockHeader.serializable.deserialize(ByteString(proto.header.toByteArray))
        val txs =
          immutable.Seq(proto.txs.map(tx => DummyTransaction.serializable.deserialize(ByteString(tx.toByteArray))): _*)
        Block(header, txs)
      }
    }
}
