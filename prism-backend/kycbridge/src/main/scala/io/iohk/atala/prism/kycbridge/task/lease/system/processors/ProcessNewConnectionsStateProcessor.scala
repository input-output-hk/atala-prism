package io.iohk.atala.prism.kycbridge.task.lease.system.processors

import cats.implicits._
import io.iohk.atala.prism.kycbridge.services.ConnectionService
import io.iohk.atala.prism.kycbridge.task.lease.system.KycBridgeProcessingTaskState
import io.iohk.atala.prism.task.lease.system.{ProcessingTask, ProcessingTaskProcessor, ProcessingTaskResult}
import monix.eval.Task

class ProcessNewConnectionsStateProcessor(connectionService: ConnectionService)
    extends ProcessingTaskProcessor[KycBridgeProcessingTaskState] {

  def process(
      processingTask: ProcessingTask[KycBridgeProcessingTaskState]
  ): Task[ProcessingTaskResult[KycBridgeProcessingTaskState]] = {
    connectionService.connectionUpdateStream.compile.drain.as(ProcessingTaskResult.ProcessingTaskRestart())
  }

}
