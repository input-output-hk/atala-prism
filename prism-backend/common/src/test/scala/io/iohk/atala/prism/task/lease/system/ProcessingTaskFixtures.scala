package io.iohk.atala.prism.task.lease.system

import io.circe.Json

import java.time.Instant

trait ProcessingTaskFixtures {
  val sampleTaskData = ProcessingTaskData(Json.fromString("sample"))

  val taskLeaseConfig = new ProcessingTaskLeaseConfig(
    leaseTimeSeconds = 30,
    extendLeaseTimeIntervalSeconds = 1,
    numberOfWorkers = 1,
    workerSleepTimeSeconds = 1
  )

  def createProcessingTask[S <: ProcessingTaskState](
      id: ProcessingTaskId = ProcessingTaskId.random(),
      state: S,
      owner: Option[ProcessingTaskOwner] = None,
      lastChange: Instant = Instant.now(),
      nextAction: Instant = Instant.now(),
      data: ProcessingTaskData = sampleTaskData
  ): ProcessingTask[S] = ProcessingTask(id, state, owner, lastChange, nextAction, data)

}
