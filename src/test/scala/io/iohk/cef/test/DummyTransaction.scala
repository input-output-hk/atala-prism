package io.iohk.cef.test
import io.iohk.cef.ledger.{LedgerError, LedgerState, Transaction}
import io.iohk.cef.utils.ByteSizeable

case class DummyTransaction(val size: Int) extends Transaction[String] {

  override def apply(v1: LedgerState[String]): Either[LedgerError, LedgerState[String]] = Right(v1)
  override def partitionIds: Set[String] = Set()

  override def toString(): String = s"DummyTransaction($size)"

  override def hashCode(): Int = size.hashCode()

  override def canEqual(that: Any): Boolean = that.isInstanceOf[DummyTransaction]

  override def equals(obj: scala.Any): Boolean = {
    obj match {
      case that: DummyTransaction => canEqual(that) && that.size == this.size
      case _ => false
    }
  }
}

object DummyTransaction {
  implicit val sizeable = new ByteSizeable[DummyTransaction] {
    override def sizeInBytes(t: DummyTransaction): Int = t.size
  }
}
