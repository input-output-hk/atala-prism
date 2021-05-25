package io.iohk.atala.prism.task.lease.system

import io.circe.Json
import io.iohk.atala.prism.models.UUIDValue

import java.time.Instant
import java.util.UUID

case class ProcessingTask(
    id: ProcessingTaskId,
    state: ProcessingTaskState,
    owner: Option[UUID],
    lastChange: Instant,
    nextAction: Instant,
    data: ProcessingTaskData
)

case class ProcessingTaskData(json: Json) extends AnyVal

case class ProcessingTaskId(uuid: UUID) extends AnyVal with UUIDValue

object ProcessingTaskId extends UUIDValue.Builder[ProcessingTaskId]
