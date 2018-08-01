package io.iohk.cef.ledger

trait Transaction[S] extends (Partitioned[S] => Either[LedgerError, Partitioned[S]]) {
  /**
    * The ids of the state partitions that need to be retrieved for this tx.
    * See [[Partitioned]] for more detail.
    *
    * @return
    */
  def partitionIds: Set[String]
}
