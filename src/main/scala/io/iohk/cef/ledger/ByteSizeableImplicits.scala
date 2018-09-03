package io.iohk.cef.ledger
import io.iohk.cef.utils.ByteSizeable

object ByteSizeableImplicits {

  implicit def byteSizeable[State, Header <: BlockHeader](
      implicit blockSerializer: ByteStringSerializable[Block[State, Header, Transaction[State]]])
    : ByteSizeable[Block[State, Header, Transaction[State]]] =
    new ByteSizeable[Block[State, Header, Transaction[State]]] {
      override def sizeInBytes(t: Block[State, Header, Transaction[State]]): Int = blockSerializer.serialize(t).size
    }
}
