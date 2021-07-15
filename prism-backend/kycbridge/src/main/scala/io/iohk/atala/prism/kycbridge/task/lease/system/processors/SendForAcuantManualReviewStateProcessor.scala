package io.iohk.atala.prism.kycbridge.task.lease.system.processors

import java.util.Base64

import monix.eval.Task
import cats.data.EitherT
import org.slf4j.LoggerFactory
import io.circe.syntax._

import io.iohk.atala.prism.kycbridge.task.lease.system.KycBridgeProcessingTaskState
import io.iohk.atala.prism.task.lease.system.{ProcessingTask, ProcessingTaskProcessor, ProcessingTaskResult}
import io.iohk.atala.prism.kycbridge.services.{AssureIdService, IdentityMindService}
import io.iohk.atala.prism.task.lease.system.ProcessingTaskProcessorOps._
import io.iohk.atala.prism.kycbridge.task.lease.system.data.SendForAcuantManualReviewStateData
import io.iohk.atala.prism.kycbridge.models.identityMind.PostConsumerRequest
import io.iohk.atala.prism.kycbridge.config.IdentityMindConfig
import io.iohk.atala.prism.task.lease.system.ProcessingTaskData
import java.time.Instant
import java.time.temporal.ChronoUnit
import io.iohk.atala.prism.kycbridge.task.lease.system.data.SendForAcuantManualReviewPendingStateData

class SendForAcuantManualReviewStateProcessor(
    assureIdService: AssureIdService,
    identityMindService: IdentityMindService,
    identityMindConfig: IdentityMindConfig
) extends ProcessingTaskProcessor[KycBridgeProcessingTaskState] {

  private implicit val logger = LoggerFactory.getLogger(this.getClass)

  def process(
      processingTask: ProcessingTask[KycBridgeProcessingTaskState],
      workerNumber: Int
  ): Task[ProcessingTaskResult[KycBridgeProcessingTaskState]] = {
    (for {
      acuantData <-
        parseProcessingTaskData[SendForAcuantManualReviewStateData, KycBridgeProcessingTaskState](processingTask)

      documentFront <- EitherT(
        assureIdService
          .getImageFromDocument(acuantData.documentInstanceId, side = "FRONT")
          .logErrorIfPresent
          .mapErrorToProcessingTaskScheduled(processingTask)
      )

      documentBack <- EitherT(
        assureIdService
          .getImageFromDocument(acuantData.documentInstanceId, side = "BACK")
          .logErrorIfPresent
          .mapErrorToProcessingTaskScheduled(processingTask)
      ).map(image => Option(image).filter(_.nonEmpty))

      consumerRequest = PostConsumerRequest(
        man = processingTask.id.uuid.toString,
        profile = identityMindConfig.profile,
        scanData = Base64.getEncoder.encodeToString(documentFront),
        backsideImageData = documentBack.map(Base64.getEncoder.encodeToString),
        faceImages = Seq(acuantData.selfieImage.value).map(Base64.getEncoder.encodeToString),
        docType = None,
        docCountry = None
      )

      response <- EitherT(
        identityMindService
          .consumer(consumerRequest)
          .logErrorIfPresent
          .mapErrorToProcessingTaskScheduled(processingTask)
      )
    } yield ProcessingTaskResult.ProcessingTaskScheduled(
      KycBridgeProcessingTaskState.SendForAcuantManualReviewPendingState,
      ProcessingTaskData(
        SendForAcuantManualReviewPendingStateData(
          receivedMessageId = acuantData.receivedMessageId,
          connectionId = acuantData.connectionId,
          documentInstanceId = acuantData.documentInstanceId,
          selfieImage = acuantData.selfieImage,
          mtid = response.mtid
        ).asJson
      ),
      scheduledTime = Instant.now().plus(10, ChronoUnit.MINUTES)
    )).value.map(_.merge)
  }

}
