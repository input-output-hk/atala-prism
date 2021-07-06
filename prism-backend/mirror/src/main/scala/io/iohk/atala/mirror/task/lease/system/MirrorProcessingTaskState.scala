package io.iohk.atala.mirror.task.lease.system

import enumeratum.Enum
import io.iohk.atala.prism.task.lease.system.ProcessingTaskState

sealed abstract class MirrorProcessingTaskState(value: String) extends ProcessingTaskState(value)

object MirrorProcessingTaskState extends Enum[MirrorProcessingTaskState] {
  lazy val values = findValues

  final case object WatchCardanoBlockchainAddressesState
      extends MirrorProcessingTaskState("WATCH_CARDANO_BLOCKCHAIN_ADDRESSES_STATE")
}
