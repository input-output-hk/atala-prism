package io.iohk.atala.prism.kycbridge.task.lease.system.processors

import cats.data.EitherT
import io.grpc.Status
import io.iohk.atala.prism.credentials.content.CredentialContent
import io.iohk.atala.prism.credentials.content.syntax._
import io.iohk.atala.prism.credentials.utils.Mustache
import io.iohk.atala.prism.credentials.{Credential, CredentialBatches}
import io.iohk.atala.prism.crypto.ECTrait
import io.iohk.atala.prism.errors.PrismError
import io.iohk.atala.prism.kycbridge.models.assureId.Document
import io.iohk.atala.prism.kycbridge.task.lease.system.KycBridgeProcessingTaskState
import io.iohk.atala.prism.kycbridge.task.lease.system.data.AcuantCreateCredentialState3Data
import io.iohk.atala.prism.kycbridge.task.lease.system.processors.AcuantCreateCredentialState3Processor.{
  CannotIssueCredentialBatch,
  HtmlTemplateError
}
import io.iohk.atala.prism.protos.credential_models
import io.iohk.atala.prism.protos.credential_models.AtalaMessage
import io.iohk.atala.prism.services.ConnectorClientService.CannotSendConnectorMessage
import io.iohk.atala.prism.services.{BaseGrpcClientService, ConnectorClientService, NodeClientService}
import io.iohk.atala.prism.task.lease.system.ProcessingTaskProcessorOps._
import io.iohk.atala.prism.task.lease.system.{ProcessingTask, ProcessingTaskProcessor, ProcessingTaskResult}
import monix.eval.Task
import org.slf4j.LoggerFactory

import java.time.format.DateTimeFormatter
import java.time.{Instant, ZoneOffset}
import java.util.Base64
import scala.io.Source
import scala.util.Try

class AcuantCreateCredentialState3Processor(
    connectorService: ConnectorClientService,
    nodeService: NodeClientService,
    authConfig: BaseGrpcClientService.DidBasedAuthConfig
)(implicit ec: ECTrait)
    extends ProcessingTaskProcessor[KycBridgeProcessingTaskState] {

  private implicit val logger = LoggerFactory.getLogger(this.getClass)

  override def process(
      processingTask: ProcessingTask[KycBridgeProcessingTaskState],
      workerNumber: Int
  ): Task[ProcessingTaskResult[KycBridgeProcessingTaskState]] = {
    (for {
      acuantData <-
        parseProcessingTaskData[AcuantCreateCredentialState3Data, KycBridgeProcessingTaskState](processingTask)

      // create credential batch with the document
      credential <- EitherT(
        Task
          .pure(createCredential(acuantData.document, acuantData.frontScannedImage.value))
          .logErrorIfPresent
          .sendResponseOnError(connectorService, acuantData.receivedMessageId, acuantData.connectionId)
          .mapErrorToProcessingTaskFinished[KycBridgeProcessingTaskState]()
      )

      (root, proof :: _) = CredentialBatches.batch(List(credential))
      credentialResponse <- EitherT(
        nodeService
          .issueCredentialBatch(root)
          .redeem(
            ex => Left(CannotIssueCredentialBatch(ex.getMessage)),
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
            ex => Left(CannotSendConnectorMessage(ex.getMessage)),
            Right(_)
          )
          .logErrorIfPresent
          .mapErrorToProcessingTaskScheduled(processingTask)
      )

    } yield ProcessingTaskResult.ProcessingTaskFinished).value
      .map(_.merge)
  }

  private[processors] def createCredential(
      document: Document,
      frontImage: Array[Byte]
  ): Either[PrismError, Credential] = {
    for {
      credentialSubject <- createCredentialSubject(document, frontImage)
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
      document: Document,
      frontImage: Array[Byte]
  ): Either[PrismError, CredentialContent.Fields] = {

    val nationality: Option[CredentialContent.Field] = for {
      code <- document.getDataField("Nationality Code").flatMap(_.value)
      name <- document.getDataField("Nationality Name").flatMap(_.value)
    } yield "nationality" -> CredentialContent.Fields(
      "code" -> code,
      "name" -> name
    )

    // val address: Option[CredentialContent.Field] = for {
    //   _ <- document.classification.flatMap(_.`type`).flatMap(_.countryCode)
    // } yield "address" -> CredentialContent.Fields(
    //   "streetAddress" -> "Bonifraterska 12",
    //   "postalCode" -> "02213",
    //   "addressLocality" -> "Warszawa",
    //   "addressRegion" -> "Mazowieckie",
    //   "addressCountry" -> "PL"
    // )
    val address: Option[CredentialContent.Field] = None // TODO: How to get it from Acuant?

    val idDocument: Option[CredentialContent.Field] = for {
      documentType <- document.classification.flatMap(_.`type`).flatMap(_.className)
      countryCode <- document.classification.flatMap(_.`type`).flatMap(_.countryCode)
      personalNumber <- document.getDataField("Personal Number").flatMap(_.value)
      documentNumber <- document.getDataField("Document Number").flatMap(_.value)
      issuingAuthority <- document.getDataField("Issuing Authority").flatMap(_.value)
    } yield "idDocument" -> CredentialContent.Fields(
      "documentType" -> documentType,
      "personalNumber" -> personalNumber,
      "documentNumber" -> documentNumber,
      "issuingAuthority" -> issuingAuthority,
      "issuingState" -> CredentialContent.Fields(
        "code" -> countryCode
        // "name" -> "Poland" // TODO: How to get it from Acuant?
      )
    )

    val biographic: CredentialContent.Fields = IndexedSeq(
      "credentialType" -> Some("KYCCredential").map(CredentialContent.StringValue),
      "name" -> document.biographic.flatMap(_.fullName).map(CredentialContent.StringValue),
      "firstName" -> document.getDataField("First Name").flatMap(_.value).map(CredentialContent.StringValue),
      "middleName" -> document.getDataField("Middle Name").flatMap(_.value).map(CredentialContent.StringValue),
      "givenName" -> document.getDataField("Given Name").flatMap(_.value).map(CredentialContent.StringValue),
      "familyName" -> document.getDataField("Surname").flatMap(_.value).map(CredentialContent.StringValue),
      "birthDate" -> document.biographic.flatMap(_.birthDate.map(formatDate)).map(CredentialContent.StringValue),
      "sex" -> document.getDataField("Sex").flatMap(_.value).map(CredentialContent.StringValue)
    ).collect { case (key, Some(value: CredentialContent.Value)) => CredentialContent.Field(key, value) }

    val templateContext = (name: String) =>
      name match {
        case "fullname" => document.biographic.flatMap(_.fullName)
        case "birthDate" => document.biographic.flatMap(_.birthDate).map(formatDate)
        case "age" => document.biographic.flatMap(_.age)
        case "gender" => document.getDataField("Sex").flatMap(_.value)
        case "expirationDate" => document.biographic.flatMap(_.expirationDate.map(formatDate))
        case "photoSrc" =>
          Option(frontImage)
            .filter(_.nonEmpty)
            .map(Base64.getEncoder().encodeToString)
            .map(encoded => s"data:image/jpg;base64, $encoded")
        case _ => None
      }

    // Log unknown fields in JSON response
    if (document.biographic.exists(_.unknownFields.nonEmpty))
      logger.info(s"Unknown fields in Acuant DocumentBiographic: ${document.biographic.map(_.unknownFields.mkString)}")

    if (document.classification.flatMap(_.`type`).exists(_.unknownFields.nonEmpty))
      logger.info(
        s"Unknown fields in Acuant DocumentClassificationType: ${document.classification.flatMap(_.`type`).map(_.unknownFields.mkString)}"
      )

    for {
      template <-
        Try(Source.fromResource("templates/identity.html").getLines().mkString).toEither.left
          .map(e => HtmlTemplateError(e.getMessage))
      html <-
        Mustache
          .render(template, templateContext)
          .left
          .map(e => HtmlTemplateError(e.getMessage))
          .map(content => CredentialContent.Fields("html" -> content))
    } yield biographic ++ nationality ++ address ++ idDocument ++ html
  }

  private[processors] def formatDate(date: Instant): String =
    DateTimeFormatter.ofPattern("yyyy-MM-dd").format(date.atOffset(ZoneOffset.UTC))
}

object AcuantCreateCredentialState3Processor {

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
