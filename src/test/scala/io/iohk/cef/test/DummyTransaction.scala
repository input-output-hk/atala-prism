package io.iohk.cef.test
import io.iohk.cef.ledger.{LedgerError, LedgerState, Transaction}

trait TestTx extends Transaction[String]

case class DummyTransaction(id: Int) extends TestTx {

  override def apply(v1: LedgerState[String]): Either[LedgerError, LedgerState[String]] = Right(v1)
  override def partitionIds: Set[String] = Set()

  override def toString(): String = s"DummyTransaction($id)"
}
