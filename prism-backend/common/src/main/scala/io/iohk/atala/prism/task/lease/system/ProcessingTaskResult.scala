package io.iohk.atala.prism.task.lease.system

import java.time.Instant

sealed trait ProcessingTaskResult[S <: ProcessingTaskState]

object ProcessingTaskResult {
  //remove task from db
  case class ProcessingTaskFinished[S <: ProcessingTaskState]() extends ProcessingTaskResult[S]
  //dispatch task in future
  case class ProcessingTaskScheduled[S <: ProcessingTaskState](
      state: S,
      data: ProcessingTaskData,
      scheduledTime: Instant
  ) extends ProcessingTaskResult[S]
  //dispatch task immediately
  case class ProcessingTaskStateTransition[S <: ProcessingTaskState](state: S, data: ProcessingTaskData)
      extends ProcessingTaskResult[S]
  //restart task with the same state
  //useful for tasks that are supposed to run forever, but fail for some reason
  case class ProcessingTaskRestart[S <: ProcessingTaskState]() extends ProcessingTaskResult[S]
}
