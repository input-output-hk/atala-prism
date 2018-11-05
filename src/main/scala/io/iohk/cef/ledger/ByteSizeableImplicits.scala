package io.iohk.cef.ledger
import io.iohk.cef.utils.ByteSizeable
import io.iohk.cef.codecs.nio._
import io.iohk.cef.utils._

object ByteSizeableImplicits {

  implicit def byteSizeable[T](implicit serializer: NioEncDec[T]): ByteSizeable[T] =
    new ByteSizeable[T] {
      override def sizeInBytes(t: T): Int = serializer.encode(t).toArray.length
    }

  implicit def blockByteSizeable[State, Header <: BlockHeader, Tx <: Transaction[State]](
      implicit headerByteSizeable: ByteSizeable[Header],
      txByteSizeable: ByteSizeable[Tx]): ByteSizeable[Block[State, Header, Tx]] =
    new ByteSizeable[Block[State, Header, Tx]] {
      override def sizeInBytes(t: Block[State, Header, Tx]): Int =
        headerByteSizeable.sizeInBytes(t.header) + t.transactions.foldLeft(0)(_ + txByteSizeable.sizeInBytes(_))
    }
}
