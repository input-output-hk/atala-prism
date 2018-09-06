package io.iohk.cef.test
import akka.util.ByteString
import io.iohk.cef.ledger.{ByteStringSerializable, LedgerError, LedgerState, Transaction}
import io.iohk.cef.utils.ByteSizeable

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

  implicit val serializable = new ByteStringSerializable[DummyTransaction with Transaction[String]] {
    override def deserialize(bytes: ByteString): DummyTransaction = DummyTransaction(BigInt(bytes.toArray).intValue())

    override def serialize(t: DummyTransaction with Transaction[String]): ByteString = ByteString(BigInt(t.size).toByteArray)
  }
}
