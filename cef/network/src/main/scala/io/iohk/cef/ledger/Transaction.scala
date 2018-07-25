package io.iohk.cef.ledger

trait Transaction[Key, Value] extends (LedgerState[Key, Value] => Either[LedgerError, LedgerState[Key, Value]]) {
  /**
    * The ids of the state partitions that need to be retrieved for this tx.
    * The partitioning is contextual. It will change from ledger to ledger. A state is well partitioned iff:
    * (1) for any transaction, the set of partitions P it depends on can be clearly identified.
    * (2) for any transaction, P fits in memory.
    *
    * Let S be the ledger state and P be a set of partitions of S. Then:
    *   A transaction t is dependent on P iff t(P) = t(S)
    * Meaning that the application of t in P has the same effect/result than the application of t in S.
    * @return
    */
  def partitionIds: Set[Key]
}
