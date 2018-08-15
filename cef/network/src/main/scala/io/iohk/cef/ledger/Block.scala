package io.iohk.cef.ledger

import scala.collection.immutable

case class Block[S,
                 Header <: BlockHeader,
                 Tx <: Transaction[S]](header: Header, transactions: immutable.Seq[Tx with Transaction[S]])
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
}
