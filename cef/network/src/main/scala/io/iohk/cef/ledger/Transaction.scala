package io.iohk.cef.ledger

trait Transaction[S] extends (LedgerState[S] => Either[LedgerError, LedgerState[S]]) {
  /**
    * The ids of the state partitions that need to be retrieved for this tx.
    * See [[LedgerState]] for more detail.
    *
    * @return
    */
  def partitionIds: Set[String]
}
