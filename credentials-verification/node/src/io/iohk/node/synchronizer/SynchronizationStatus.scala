package io.iohk.node.synchronizer

private[synchronizer] sealed trait SynchronizationStatus

private[synchronizer] object SynchronizationStatus {

  /**
    * Already synced.
    */
  final case object Synced extends SynchronizationStatus

  /**
    * The ledger is missing a block interval which can be applied sequentially.
    *
    * @param range the missing interval
    */
  final case class MissingBlockInterval(range: Range) extends SynchronizationStatus

  /**
    * The ledger needs a reorganization.
    *
    * @param cutPoint the new latest block after applying the rollback
    * @param goal the goal to reach after rolling back
    */
  final case class PendingReorganization(cutPoint: BlockPointer, goal: Int) extends SynchronizationStatus {
    require(goal > cutPoint.height, "The goal must go after the cut point")
  }
}
