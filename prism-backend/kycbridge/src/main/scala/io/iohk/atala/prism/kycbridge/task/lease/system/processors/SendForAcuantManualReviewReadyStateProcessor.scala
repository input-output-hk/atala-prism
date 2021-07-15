package io.iohk.atala.prism.kycbridge.task.lease.system.processors

import java.util.Base64

import scala.io.Source
import scala.util.Try

import monix.eval.Task
import cats.data.EitherT
import org.slf4j.LoggerFactory
import io.grpc.Status

import io.iohk.atala.prism.kycbridge.task.lease.system.KycBridgeProcessingTaskState
import io.iohk.atala.prism.task.lease.system.{ProcessingTask, ProcessingTaskProcessor, ProcessingTaskResult}
import io.iohk.atala.prism.task.lease.system.ProcessingTaskProcessorOps._

import io.iohk.atala.prism.kycbridge.task.lease.system.data.SendForAcuantManualReviewReadyStateData
import io.iohk.atala.prism.kycbridge.models.identityMind.{GetConsumerRequest, GetConsumerResponse}
import io.iohk.atala.prism.errors.PrismError
import io.iohk.atala.prism.credentials.Credential
import io.iohk.atala.prism.crypto.ECTrait
import io.iohk.atala.prism.services.{ConnectorClientService, NodeClientService, BaseGrpcClientService}
import io.iohk.atala.prism.kycbridge.services.IdentityMindService
import io.iohk.atala.prism.credentials.CredentialBatches
import io.iohk.atala.prism.protos.credential_models
import io.iohk.atala.prism.protos.credential_models.AtalaMessage
import io.iohk.atala.prism.credentials.content.CredentialContent
import io.iohk.atala.prism.credentials.content.syntax._
import io.iohk.atala.prism.credentials.utils.Mustache

class SendForAcuantManualReviewReadyStateProcessor(
    connectorService: ConnectorClientService,
    nodeService: NodeClientService,
    identityMindService: IdentityMindService,
    authConfig: BaseGrpcClientService.DidBasedAuthConfig
)(implicit ec: ECTrait)
    extends ProcessingTaskProcessor[KycBridgeProcessingTaskState] {

  private implicit val logger = LoggerFactory.getLogger(this.getClass)

  private val credentialTemplate = Source.fromResource("templates/identity.html").getLines().mkString

  def process(
      processingTask: ProcessingTask[KycBridgeProcessingTaskState],
      workerNumber: Int
  ): Task[ProcessingTaskResult[KycBridgeProcessingTaskState]] = {
    (for {
      acuantData <-
        parseProcessingTaskData[SendForAcuantManualReviewReadyStateData, KycBridgeProcessingTaskState](processingTask)

      consumerRequest = GetConsumerRequest(
        mtid = acuantData.mtid
      )

      consumerResponse <- EitherT(
        identityMindService
          .consumer(consumerRequest)
          .logErrorIfPresent
          .mapErrorToProcessingTaskScheduled(processingTask)
      )

      // create credential batch with the document
      credential <- EitherT(
        Task
          .pure(createCredential(consumerResponse, acuantData.selfieImage.value))
          .logErrorIfPresent
          .sendResponseOnError(connectorService, acuantData.receivedMessageId, acuantData.connectionId)
          .mapErrorToProcessingTaskFinished[KycBridgeProcessingTaskState]()
      )

      (root, proof :: _) = CredentialBatches.batch(List(credential))
      credentialResponse <- EitherT(
        nodeService
          .issueCredentialBatch(root)
          .redeem(
            ex => Left(SendForAcuantManualReviewReadyStateProcessor.CannotIssueCredentialBatch(ex.getMessage)),
            Right(_)
          )
          .logErrorIfPresent
          .mapErrorToProcessingTaskScheduled(processingTask)
      )

      // send credential along with inclusion proof
      credentialProto = credential_models.PlainTextCredential(
        encodedCredential = credential.canonicalForm,
        encodedMerkleProof = proof.encode
      )
      atalaMessage = AtalaMessage().withPlainCredential(credentialProto)

      _ = logger.info(
        s"Credential batch created for document instance id: ${acuantData.documentInstanceId} with batch id: ${credentialResponse.batchId}"
      )

      _ <- EitherT(
        connectorService
          .sendResponseMessage(atalaMessage, acuantData.receivedMessageId, acuantData.connectionId)
          .redeem(
            ex => Left(ConnectorClientService.CannotSendConnectorMessage(ex.getMessage)),
            Right(_)
          )
          .logErrorIfPresent
          .mapErrorToProcessingTaskScheduled(processingTask)
      )

    } yield ProcessingTaskResult.ProcessingTaskFinished).value.map(_.merge)
  }

  private[processors] def createCredential(
      consumer: GetConsumerResponse,
      frontImage: Array[Byte]
  ): Either[PrismError, Credential] = {
    for {
      credentialSubject <- createCredentialSubject(consumer, frontImage)
    } yield Credential
      .fromCredentialContent(
        CredentialContent(
          CredentialContent.JsonFields.CredentialType.field -> "KYCCredential",
          CredentialContent.JsonFields.Issuer.field -> authConfig.did.value,
          CredentialContent.JsonFields.IssuanceKeyId.field -> authConfig.didIssuingKeyId,
          CredentialContent.JsonFields.CredentialSubject.field -> credentialSubject
        )
      )
      .sign(authConfig.didIssuingKeyPair.privateKey)
  }

  private[processors] def createCredentialSubject(
      consumer: GetConsumerResponse,
      frontImage: Array[Byte]
  ): Either[PrismError, CredentialContent.Fields] = {

    val nationality: Option[CredentialContent.Field] = for {
      code <- consumer.getDataField(GetConsumerResponse.CountryOfId).flatMap(_.details)
    } yield "nationality" -> CredentialContent.Fields(
      "code" -> code
    )
    val address: Option[CredentialContent.Field] =
      consumer
        .getDataField(GetConsumerResponse.Address)
        .flatMap(_.details)
        .filterNot(_ == GetConsumerResponse.INVALID_ADDRESS)
        .map(address => "address" -> address)

    val idDocument: Option[CredentialContent.Field] = for {
      documentType <- consumer.getDataField(GetConsumerResponse.TypeOfDocument).flatMap(_.details)
      countryCode <- consumer.getDataField(GetConsumerResponse.CountryOfId).flatMap(_.details)
      documentNumber <- consumer.getDataField(GetConsumerResponse.DocumentNumber).flatMap(_.details)
    } yield "idDocument" -> CredentialContent.Fields(
      "documentType" -> documentType,
      "documentNumber" -> documentNumber,
      "issuingState" -> CredentialContent.Fields(
        "code" -> countryCode
      )
    )

    val biographic: CredentialContent.Fields = IndexedSeq(
      "credentialType" -> Some("KYCCredential").map(CredentialContent.StringValue),
      "name" -> consumer.getDataField(GetConsumerResponse.Name).flatMap(_.details).map(CredentialContent.StringValue)
    ).collect { case (key, Some(value: CredentialContent.Value)) => CredentialContent.Field(key, value) }

    val templateContext = (name: String) =>
      name match {
        case "fullname" => consumer.getDataField(GetConsumerResponse.Name).flatMap(_.details)
        case "expirationDate" =>
          consumer
            .getDataField(GetConsumerResponse.ExpirationDate)
            .flatMap(_.details)
        case "photoSrc" =>
          Option(frontImage)
            .filter(_.nonEmpty)
            .map(Base64.getEncoder().encodeToString)
            .map(encoded => s"data:image/jpg;base64, $encoded")
        case _ => None
      }
    for {
      template <-
        Try(credentialTemplate).toEither.left
          .map(e => SendForAcuantManualReviewReadyStateProcessor.HtmlTemplateError(e.getMessage))
      html <-
        Mustache
          .render(template, templateContext, validate = false) // TODO: Set validate to true when we'll have more data
          .left
          .map(e => SendForAcuantManualReviewReadyStateProcessor.HtmlTemplateError(e.getMessage))
          .map(content => CredentialContent.Fields("html" -> content))
    } yield biographic ++ nationality ++ address ++ idDocument ++ html
  }
}

object SendForAcuantManualReviewReadyStateProcessor {

  case class CannotIssueCredentialBatch(message: String) extends PrismError {
    override def toStatus: Status = {
      Status.INTERNAL.withDescription(
        s"Failed issuing credential batch: $message"
      )
    }
  }

  case class HtmlTemplateError(message: String) extends PrismError {
    override def toStatus: Status = {
      Status.INTERNAL.withDescription(
        s"Credential html Mustache error: $message"
      )
    }
  }

}
