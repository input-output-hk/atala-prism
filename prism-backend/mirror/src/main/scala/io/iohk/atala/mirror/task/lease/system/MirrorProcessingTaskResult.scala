package io.iohk.atala.mirror.task.lease.system

import io.iohk.atala.prism.task.lease.system.ProcessingTaskResult

object MirrorProcessingTaskResult {
  type MirrorProcessingTaskResult = ProcessingTaskResult[MirrorProcessingTaskState]
}
