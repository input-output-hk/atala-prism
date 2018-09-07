package io.iohk.cef.ledger
import io.iohk.cef.utils.ByteSizeable

object ByteSizeableImplicits {

  implicit def byteSizeable[T](implicit serializer: ByteStringSerializable[T]): ByteSizeable[T] =
    new ByteSizeable[T] {
      override def sizeInBytes(t: T): Int = serializer.serialize(t).size
    }

  implicit def blockByteSizeable[State, Header <: BlockHeader, Tx <: Transaction[State]](
      implicit headerByteSizeable: ByteSizeable[Header],
      txByteSizeable: ByteSizeable[Tx]): ByteSizeable[Block[State, Header, Tx]] =
    new ByteSizeable[Block[State, Header, Tx]] {
      override def sizeInBytes(t: Block[State, Header, Tx]): Int =
        headerByteSizeable.sizeInBytes(t.header) + t.transactions.foldLeft(0)(_ + txByteSizeable.sizeInBytes(_))
    }
}
