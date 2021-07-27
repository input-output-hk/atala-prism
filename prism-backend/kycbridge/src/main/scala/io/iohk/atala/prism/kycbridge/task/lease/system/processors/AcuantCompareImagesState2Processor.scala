package io.iohk.atala.prism.kycbridge.task.lease.system.processors

import cats.data.EitherT
import cats.syntax.functor._
import io.circe.syntax._
import io.grpc.Status
import io.iohk.atala.prism.errors.PrismError
import io.iohk.atala.prism.kycbridge.models.faceId
import io.iohk.atala.prism.kycbridge.models.faceId.FaceMatchResponse
import io.iohk.atala.prism.kycbridge.services.{AssureIdService, FaceIdService}
import io.iohk.atala.prism.kycbridge.task.lease.system.KycBridgeProcessingTaskState
import io.iohk.atala.prism.kycbridge.task.lease.system.data.{
  AcuantCompareImagesState2Data,
  AcuantCreateCredentialState3Data
}
import io.iohk.atala.prism.kycbridge.task.lease.system.processors.AcuantCompareImagesState2Processor.FaceMatchFailedError
import io.iohk.atala.prism.services.ConnectorClientService
import io.iohk.atala.prism.task.lease.system.ProcessingTaskProcessorOps._
import io.iohk.atala.prism.task.lease.system.{
  ProcessingTask,
  ProcessingTaskData,
  ProcessingTaskProcessor,
  ProcessingTaskResult
}
import monix.eval.Task
import org.slf4j.LoggerFactory

class AcuantCompareImagesState2Processor(
    connectorService: ConnectorClientService,
    assureIdService: AssureIdService,
    faceIdService: FaceIdService
) extends ProcessingTaskProcessor[KycBridgeProcessingTaskState] {

  private implicit val logger = LoggerFactory.getLogger(this.getClass)

  override def process(
      processingTask: ProcessingTask[KycBridgeProcessingTaskState],
      workerNumber: Int
  ): Task[ProcessingTaskResult[KycBridgeProcessingTaskState]] = {
    (for {
      acuantData <- parseProcessingTaskData[AcuantCompareImagesState2Data, KycBridgeProcessingTaskState](processingTask)

      frontScannedImage <- EitherT(
        assureIdService
          .getFrontImageFromDocument(acuantData.document.instanceId)
          .logErrorIfPresent
          .mapErrorToProcessingTaskScheduled(processingTask)
      )

      faceMatchData = faceId.Data(frontScannedImage, acuantData.selfieImage.value)

      faceMatchResult <- EitherT(
        faceIdService
          .faceMatch(faceMatchData)
          .logErrorIfPresent
          .mapErrorToProcessingTaskScheduled(processingTask)
      )

      result <-
        EitherT.right[ProcessingTaskResult[KycBridgeProcessingTaskState]](Task(faceMatchResult.isMatch).flatMap {
          case true =>
            val data = AcuantCreateCredentialState3Data.fromAcuantCompareImagesState2Data(acuantData, frontScannedImage)
            Task.pure(
              ProcessingTaskResult
                .ProcessingTaskStateTransition(
                  KycBridgeProcessingTaskState.AcuantIssueCredentialState3,
                  ProcessingTaskData(data.asJson)
                )
            )
          case false =>
            logger.warn(
              s"Face match failed with score ${faceMatchResult.score}, document image size: ${frontScannedImage.size}, selfie image size: ${acuantData.selfieImage.value.size}"
            )
            connectorService
              .sendResponseMessage(
                message = FaceMatchFailedError(faceMatchResult).toAtalaMessage,
                receivedMessageId = acuantData.receivedMessageId,
                connectionId = acuantData.connectionId
              )
              .as(ProcessingTaskResult.ProcessingTaskFinished)
        })
    } yield result).value
      .map(_.merge)
  }
}

object AcuantCompareImagesState2Processor {
  case class FaceMatchFailedError(faceMatchResponse: FaceMatchResponse) extends PrismError {
    override def toStatus: Status = {
      Status.INVALID_ARGUMENT.withDescription(
        s"User's selfie doesn't match photo extracted from document, face id score ${faceMatchResponse.score}"
      )
    }
  }
}
