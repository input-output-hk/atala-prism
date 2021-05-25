package io.iohk.atala.prism.task.lease.system.processors

import io.circe.Json
import io.iohk.atala.prism.task.lease.system.{
  ProcessingTask,
  ProcessingTaskData,
  ProcessingTaskResult,
  ProcessingTaskService
}
import monix.eval.Task

object TestState1Processor {

  def process(
      processingTask: ProcessingTask,
      processingTaskService: ProcessingTaskService
  ): Task[ProcessingTaskResult] = {
    println(processingTask)
    processingTaskService
      .updateData(
        processingTask.id,
        ProcessingTaskData(Json.obj("sampleKey" -> Json.fromString("sampleValue")))
      )
      .map(_ => ProcessingTaskResult.ProcessingTaskFinished)
  }

}
