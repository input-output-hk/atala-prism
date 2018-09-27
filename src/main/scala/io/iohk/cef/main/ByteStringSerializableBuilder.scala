package io.iohk.cef.main
import io.iohk.cef.ledger.{Block, BlockHeader, ByteStringSerializable, Transaction}

trait ByteStringSerializableBuilder[S, H <: BlockHeader, T <: Transaction[S]] {
  implicit val byteStringSerializable: ByteStringSerializable[Block[S, H, T]]
}
