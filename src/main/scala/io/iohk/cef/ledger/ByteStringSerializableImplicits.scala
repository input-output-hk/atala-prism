package io.iohk.cef.ledger
import akka.util.ByteString
import io.iohk.cef.protobuf.Common.BlockProto

import scala.collection.immutable

object ByteStringSerializableImplicits {
  implicit def blockByteStringSerializable[State, Header <: BlockHeader, Tx <: Transaction[State]](
      implicit headerSerializable: ByteStringSerializable[Header],
      txSerializable: ByteStringSerializable[Tx with Transaction[State]]): ByteStringSerializable[Block[State, Header, Tx]] =
    new ByteStringSerializable[Block[State, Header, Tx]] {
      override def serialize(t: Block[State, Header, Tx]): ByteString = {
        val proto = BlockProto(com.google.protobuf.ByteString.copyFrom(headerSerializable.serialize(t.header).toArray),
          t.transactions.map(tx => com.google.protobuf.ByteString.copyFrom(txSerializable.serialize(tx).toArray)))
        ByteString(proto.toByteArray)
      }
      override def deserialize(bytes: ByteString): Block[State, Header, Tx] = {
        val proto = BlockProto.parseFrom(bytes.toArray)
        val txs = immutable.Seq(proto.txs.map(tx => txSerializable.deserialize(ByteString(tx.toByteArray))):_*)
        val header = headerSerializable.deserialize(ByteString(proto.header.toByteArray))
        Block[State, Header, Tx](header, txs)
      }
    }
}
