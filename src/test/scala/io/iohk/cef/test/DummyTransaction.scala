package io.iohk.cef.test
import io.iohk.cef.ledger.{LedgerError, LedgerState, Transaction}
import io.iohk.cef.utils.ByteSizeable
import io.iohk.cef.codecs.nio._
import io.iohk.cef.utils._
import java.nio.ByteBuffer

import scala.util.Try

trait TestTx extends Transaction[String]

case class DummyTransaction(val size: Int) extends TestTx {

  override def apply(v1: LedgerState[String]): Either[LedgerError, LedgerState[String]] = Right(v1)
  override def partitionIds: Set[String] = Set()

  override def toString(): String = s"DummyTransaction($size)"
}

object DummyTransaction {
  implicit val sizeable: ByteSizeable[DummyTransaction] = new ByteSizeable[DummyTransaction] {
    override def sizeInBytes(t: DummyTransaction): Int = t.size
  }

  private def decode(bb: ByteBuffer): Option[DummyTransaction] = {
    val bytes = bb.toByteString
    Try(if (bytes.forall(_ == 1)) {
      DummyTransaction(bytes.size)
    } else throw new IllegalArgumentException("Invalid format for DummyTransaction")).toOption
  }

  private def encode(t: DummyTransaction): ByteBuffer =
    Array.fill[Byte](t.size)(1).toByteBuffer

  implicit val serializable: NioEncDec[DummyTransaction] = NioEncDec[DummyTransaction](encode _, decode _)
}
