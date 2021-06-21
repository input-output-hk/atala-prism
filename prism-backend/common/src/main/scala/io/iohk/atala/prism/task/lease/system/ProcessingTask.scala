package io.iohk.atala.prism.task.lease.system

import io.circe.Json
import io.iohk.atala.prism.models.UUIDValue

import java.time.Instant
import java.util.UUID

case class ProcessingTask[S <: ProcessingTaskState](
    id: ProcessingTaskId,
    state: S,
    owner: Option[ProcessingTaskOwner],
    lastChange: Instant,
    nextAction: Instant,
    data: ProcessingTaskData
)

case class ProcessingTaskData(json: Json) extends AnyVal

case class ProcessingTaskId(uuid: UUID) extends AnyVal with UUIDValue

case class ProcessingTaskOwner(owner: String) extends AnyVal

object ProcessingTaskId extends UUIDValue.Builder[ProcessingTaskId]
