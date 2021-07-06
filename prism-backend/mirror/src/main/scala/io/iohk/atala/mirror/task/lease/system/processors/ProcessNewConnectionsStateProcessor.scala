package io.iohk.atala.mirror.task.lease.system.processors

import cats.implicits._
import io.iohk.atala.mirror.services.CredentialService
import io.iohk.atala.mirror.task.lease.system.MirrorProcessingTaskState
import io.iohk.atala.prism.models.CredentialProofRequestType
import io.iohk.atala.prism.task.lease.system.{ProcessingTask, ProcessingTaskProcessor, ProcessingTaskResult}
import monix.eval.Task

class ProcessNewConnectionsStateProcessor(credentialService: CredentialService)
    extends ProcessingTaskProcessor[MirrorProcessingTaskState] {

  def process(
      processingTask: ProcessingTask[MirrorProcessingTaskState],
      workerNumber: Int
  ): Task[ProcessingTaskResult[MirrorProcessingTaskState]] = {
    credentialService
      .connectionUpdatesStream(
        // TODO: We are sending unsigned credential form intdemo by default, it allows
        //       to test the Mirror with the mobile apps, to check signed flow, see: [[MirrorE2eSpec]].
        immediatelyRequestedCredential = CredentialProofRequestType.RedlandIdCredential
      )
      .compile
      .drain
      .as(ProcessingTaskResult.ProcessingTaskRestart)
  }

}
