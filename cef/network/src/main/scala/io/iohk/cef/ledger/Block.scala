package io.iohk.cef.ledger

case class Block[S,
                 Header <: BlockHeader,
                 Tx <: Transaction[S]](header: Header, transactions: List[Tx with Transaction[S]])
    extends (Partitioned[S] => Either[LedgerError, Partitioned[S]]) {

  override def apply(state: Partitioned[S]): Either[LedgerError, Partitioned[S]] = {
    transactions.foldLeft[Either[LedgerError, Partitioned[S]]](Right(state))((either, tx) => {
      either.flatMap(tx(_))
    })
  }

  /**
    * See the doc in Transaction
    */
  def partitionIds: Set[String] = {
    transactions.foldLeft[Set[String]](Set())(_ ++ _.partitionIds)
  }
}
