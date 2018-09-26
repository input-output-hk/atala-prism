package io.iohk.cef.test
import akka.util.ByteString
import io.iohk.cef.ledger.{ByteStringSerializable, LedgerError, LedgerState, Transaction}
import io.iohk.cef.utils.ByteSizeable

import scala.util.Try

trait TestTx extends Transaction[String]

case class DummyTransaction(val size: Int) extends TestTx {

  override def apply(v1: LedgerState[String]): Either[LedgerError, LedgerState[String]] = Right(v1)
  override def partitionIds: Set[String] = Set()

  override def toString(): String = s"DummyTransaction($size)"
}

object DummyTransaction {
  implicit val sizeable = new ByteSizeable[DummyTransaction] {
    override def sizeInBytes(t: DummyTransaction): Int = t.size
  }

  implicit val serializable = new ByteStringSerializable[DummyTransaction] {
    override def decode(bytes: ByteString): Option[DummyTransaction] =
      Try(
        if(bytes.forall(_ == 1)) {
          DummyTransaction(bytes.size)
        } else throw new IllegalArgumentException("Invalid format for DummyTransaction")).toOption

    override def encode(t: DummyTransaction): ByteString =
      ByteString(Array.fill[Byte](t.size)(1))
  }
}
