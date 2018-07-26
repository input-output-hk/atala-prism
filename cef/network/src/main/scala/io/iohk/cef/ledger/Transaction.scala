package io.iohk.cef.ledger

trait Transaction[S] extends (LedgerState[S] => Either[LedgerError, LedgerState[S]]) {
  /**
    * The ids of the state partitions that need to be retrieved for this tx.
    * The partitioning is contextual. It will change from ledger to ledger. A state is well partitioned iff:
    * (1) for any transaction, the set of partitions P it depends on can be clearly identified.
    * (2) for any transaction, P fits in memory.
    *
    * Let S be the ledger state and Q be a set of partitions of S. Then:
    *   A transaction t is dependent on Q iff t(S - Q) != t(S)
    * Meaning that the application of t in S without Q results in a different scenario than applying t on S.
    * @return
    */
  def partitionIds: Set[String]
}
