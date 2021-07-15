package io.iohk.atala.prism.kycbridge.task.lease.system.processors

import monix.eval.Task
import cats.data.EitherT
import org.slf4j.LoggerFactory
import io.circe.syntax._
import cats.syntax.functor._

import io.iohk.atala.prism.kycbridge.task.lease.system.KycBridgeProcessingTaskState
import io.iohk.atala.prism.task.lease.system.{ProcessingTask, ProcessingTaskProcessor, ProcessingTaskResult}
import io.iohk.atala.prism.kycbridge.services.IdentityMindService
import io.iohk.atala.prism.task.lease.system.ProcessingTaskProcessorOps._
import io.iohk.atala.prism.kycbridge.task.lease.system.data.SendForAcuantManualReviewPendingStateData
import io.iohk.atala.prism.kycbridge.models.identityMind.GetConsumerRequest
import java.time.Instant
import java.time.temporal.ChronoUnit
import io.iohk.atala.prism.kycbridge.task.lease.system.data.SendForAcuantManualReviewReadyStateData
import io.iohk.atala.prism.task.lease.system.ProcessingTaskData
import io.iohk.atala.prism.services.ConnectorClientService
import io.grpc.Status
import io.iohk.atala.prism.errors.PrismError
import io.iohk.atala.prism.kycbridge.models.identityMind.ConsumerResponseState

class SendForAcuantManualReviewPendingStateProcessor(
    connectorService: ConnectorClientService,
    identityMindService: IdentityMindService
) extends ProcessingTaskProcessor[KycBridgeProcessingTaskState] {

  private implicit val logger = LoggerFactory.getLogger(this.getClass)

  def process(
      processingTask: ProcessingTask[KycBridgeProcessingTaskState],
      workerNumber: Int
  ): Task[ProcessingTaskResult[KycBridgeProcessingTaskState]] = {
    (for {
      acuantData <-
        parseProcessingTaskData[SendForAcuantManualReviewPendingStateData, KycBridgeProcessingTaskState](processingTask)

      consumerRequest = GetConsumerRequest(
        mtid = acuantData.mtid
      )

      consumerResponse <- EitherT(
        identityMindService
          .consumer(consumerRequest)
          .logErrorIfPresent
          .mapErrorToProcessingTaskScheduled(processingTask)
      )

      result <- EitherT.right[ProcessingTaskResult[KycBridgeProcessingTaskState]](Task(consumerResponse.state).flatMap {
        case ConsumerResponseState.Accept =>
          Task.pure(
            ProcessingTaskResult
              .ProcessingTaskStateTransition(
                KycBridgeProcessingTaskState.SendForAcuantManualReviewReadyState,
                ProcessingTaskData(
                  SendForAcuantManualReviewReadyStateData(
                    receivedMessageId = acuantData.receivedMessageId,
                    connectionId = acuantData.connectionId,
                    documentInstanceId = acuantData.documentInstanceId,
                    selfieImage = acuantData.selfieImage,
                    mtid = acuantData.mtid
                  ).asJson
                )
              )
          )

        case ConsumerResponseState.Deny =>
          connectorService
            .sendResponseMessage(
              message = SendForAcuantManualReviewPendingStateProcessor.ManulaReviewFailedError.toAtalaMessage,
              receivedMessageId = acuantData.receivedMessageId,
              connectionId = acuantData.connectionId
            )
            .as(ProcessingTaskResult.ProcessingTaskFinished)

        case _ =>
          Task.pure(
            ProcessingTaskResult.ProcessingTaskScheduled(
              KycBridgeProcessingTaskState.SendForAcuantManualReviewPendingState,
              processingTask.data,
              scheduledTime = Instant.now().plus(10, ChronoUnit.MINUTES)
            )
          )
      })
    } yield result).value.map(_.merge)
  }

}

object SendForAcuantManualReviewPendingStateProcessor {
  case object ManulaReviewFailedError extends PrismError {
    override def toStatus: Status = {
      Status.INVALID_ARGUMENT.withDescription(
        "Manual review for given document failed."
      )
    }
  }
}
