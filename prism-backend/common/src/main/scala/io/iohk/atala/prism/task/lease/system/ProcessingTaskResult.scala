package io.iohk.atala.prism.task.lease.system

import java.time.Instant

sealed trait ProcessingTaskResult

object ProcessingTaskResult {
  //remove task from db
  case object ProcessingTaskFinished extends ProcessingTaskResult
  //dispatch task in future
  case class ProcessingTaskScheduled(state: ProcessingTaskState, data: ProcessingTaskData, scheduledTime: Instant)
      extends ProcessingTaskResult
  //dispatch task immediately
  case class ProcessingTaskStateTransition(state: ProcessingTaskState, data: ProcessingTaskData)
      extends ProcessingTaskResult
}
