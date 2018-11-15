package io.iohk.cef.ledger

case class Block[S, Tx <: Transaction[S]](header: BlockHeader, transactions: Seq[Tx])
    extends (LedgerState[S] => Either[LedgerError, LedgerState[S]]) {

  override def apply(state: LedgerState[S]): Either[LedgerError, LedgerState[S]] = {
    transactions.foldLeft[Either[LedgerError, LedgerState[S]]](Right(state))((either, tx) => {
      either.flatMap(tx(_))
    })
  }

  /**
    * See the doc in Transaction
    */
  def partitionIds: Set[String] = {
    transactions.foldLeft[Set[String]](Set())(_ ++ _.partitionIds)
  }

  override def toString(): String = s"Block($header,$transactions)"
}
