package atala.config

import pureconfig.error._

trait ObftNodeValidator {
  def validateObftNode(candidate: ObftNode): Option[FailureReason]
}

object ObftNodeValidator {

  def apply(): ObftNodeValidator = new ObftNodeValidatorImpl()

}

class ObftNodeValidatorImpl() extends ObftNodeValidator {

  override def validateObftNode(candidate: ObftNode): Option[FailureReason] = {
    val thisIndex = candidate.serverIndex
    val remoteIndexes = candidate.remoteNodes.toList.map(_.serverIndex).sorted
    val allIndexes = thisIndex :: remoteIndexes
    val lastIndex = allIndexes.size

    if (allIndexes.sorted == (1 to lastIndex).toList)
      None
    else
      Some(
        CannotConvert(
          candidate.toString,
          "ObftNode",
          s"This node is configured with index $thisIndex and the remote nodes have indexes ${remoteIndexes
            .mkString(", ")}, where the indexes of all the nodes should go from 1 to $lastIndex"
        )
      )
  }

}
