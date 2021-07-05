package io.iohk.atala.prism.kycbridge.task.lease.system.processors

import java.util.Base64

import monix.eval.Task
import cats.data.EitherT
import org.slf4j.LoggerFactory

import io.iohk.atala.prism.kycbridge.task.lease.system.KycBridgeProcessingTaskState
import io.iohk.atala.prism.task.lease.system.{ProcessingTask, ProcessingTaskProcessor, ProcessingTaskResult}
import io.iohk.atala.prism.kycbridge.services.{AssureIdService, IdentityMindService}
import io.iohk.atala.prism.task.lease.system.ProcessingTaskProcessorOps._
import io.iohk.atala.prism.kycbridge.task.lease.system.data.SendForAcuantManualReviewStateData
import io.iohk.atala.prism.kycbridge.models.identityMind.ConsumerRequest
import io.iohk.atala.prism.kycbridge.config.IdentityMindConfig

class SendForAcuantManualReviewStateProcessor(
    assureIdService: AssureIdService,
    identityMindService: IdentityMindService,
    identityMindConfig: IdentityMindConfig
) extends ProcessingTaskProcessor[KycBridgeProcessingTaskState] {

  private implicit val logger = LoggerFactory.getLogger(this.getClass)

  def process(
      processingTask: ProcessingTask[KycBridgeProcessingTaskState]
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

      consumerRequest = ConsumerRequest(
        man = processingTask.id.uuid.toString,
        profile = identityMindConfig.profile,
        scanData = Base64.getEncoder.encodeToString(documentFront),
        backsideImageData = documentBack.map(Base64.getEncoder.encodeToString),
        faceImages = Seq(acuantData.selfieImage.value).map(Base64.getEncoder.encodeToString),
        docType = None,
        docCountry = None
      )

      _ <- EitherT(
        identityMindService
          .consumer(consumerRequest)
          .logErrorIfPresent
          .mapErrorToProcessingTaskScheduled(processingTask)
      )
    } yield ProcessingTaskResult.ProcessingTaskFinished).value.map(_.merge)
  }

}
