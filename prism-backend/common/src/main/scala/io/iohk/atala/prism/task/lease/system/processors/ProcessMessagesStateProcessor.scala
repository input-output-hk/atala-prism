package io.iohk.atala.prism.task.lease.system.processors

import io.iohk.atala.prism.services.ConnectorMessagesService
import io.iohk.atala.prism.task.lease.system.{
  ProcessingTask,
  ProcessingTaskProcessor,
  ProcessingTaskResult,
  ProcessingTaskState
}
import monix.eval.Task

class ProcessMessagesStateProcessor[S <: ProcessingTaskState](connectorMessagesService: ConnectorMessagesService)
    extends ProcessingTaskProcessor[S] {

  def process(processingTask: ProcessingTask[S], workerNumber: Int): Task[ProcessingTaskResult[S]] = {
    connectorMessagesService.messagesUpdatesStream.compile.drain.as(ProcessingTaskResult.ProcessingTaskRestart)
  }

}
