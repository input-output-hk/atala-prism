package io.iohk.atala.prism.kycbridge.processors

import java.time.{Instant, ZoneOffset}
import java.time.format.DateTimeFormatter
import java.util.Base64

import scala.util.Try
import scala.io.Source

import monix.eval.Task
import cats.data.EitherT
import doobie.util.transactor.Transactor
import org.slf4j.LoggerFactory
import doobie.implicits._
import io.grpc.Status

import io.iohk.atala.prism.services.{BaseGrpcClientService, MessageProcessor, NodeClientService}
import io.iohk.atala.prism.protos.connector_models.ReceivedMessage
import io.iohk.atala.prism.protos.credential_models
import io.iohk.atala.prism.protos.credential_models.{AcuantProcessFinished, AtalaMessage}
import io.iohk.atala.prism.kycbridge.db.ConnectionDao
import io.iohk.atala.prism.kycbridge.models.assureId.Document
import io.iohk.atala.prism.kycbridge.services.{AssureIdService, FaceIdService}
import io.iohk.atala.prism.credentials.{Credential, CredentialBatches}
import io.iohk.atala.prism.credentials.content.CredentialContent
import io.iohk.atala.prism.credentials.content.syntax._
import io.iohk.atala.prism.crypto.ECTrait
import io.iohk.atala.prism.kycbridge.models.faceId
import io.iohk.atala.prism.errors.PrismError
import io.iohk.atala.prism.kycbridge.models.faceId.FaceMatchResponse
import io.iohk.atala.prism.kycbridge.processors.DocumentUploadedMessageProcessor.{
  CannotIssueCredentialBatch,
  FaceMatchFailedError,
  HtmlTemplateError
}
import io.iohk.atala.prism.protos.node_api.IssueCredentialBatchResponse
import io.iohk.atala.prism.utils.ConnectionUtils
import io.iohk.atala.prism.utils.syntax.DBConnectionOps
import io.iohk.atala.prism.credentials.utils.Mustache

class DocumentUploadedMessageProcessor(
    tx: Transactor[Task],
    nodeService: NodeClientService,
    assureIdService: AssureIdService,
    faceIdService: FaceIdService,
    authConfig: BaseGrpcClientService.DidBasedAuthConfig
)(implicit ec: ECTrait) {

  private val logger = LoggerFactory.getLogger(this.getClass)

  val processor: MessageProcessor = { receivedMessage =>
    parseAcuantProcessFinishedMessage(receivedMessage)
      .map { message =>
        logger.info(s"Processing message with document instance id: ${message.documentInstanceId}")
        (for {
          // get required information
          connection <- EitherT(
            ConnectionUtils
              .fromReceivedMessage(receivedMessage, ConnectionDao.findByConnectionId)
              .logSQLErrors("getting connection from received message", logger)
              .transact(tx)
          )
          documentStatus <- EitherT(assureIdService.getDocumentStatus(message.documentInstanceId))

          document <- EitherT(assureIdService.getDocument(message.documentInstanceId))
          frontImage <- EitherT(assureIdService.getFrontImageFromDocument(message.documentInstanceId))

          // update connection with new document status
          _ <- EitherT.right[PrismError](
            ConnectionDao
              .update(connection.copy(acuantDocumentStatus = Some(documentStatus)))
              .logSQLErrors("updating connection", logger)
              .transact(tx)
          )

          frontScannedImage <- EitherT(assureIdService.getFrontImageFromDocument(document.instanceId))

          faceMatchData = faceId.Data(frontScannedImage, message.selfieImage.toByteArray)

          faceMatchResult <- EitherT(faceIdService.faceMatch(faceMatchData))

          _ <- EitherT.cond[Task](
            faceMatchResult.isMatch,
            (),
            FaceMatchFailedError(faceMatchResult)
          )

          // create credential batch with the document
          credential <- EitherT.fromEither[Task](createCredential(document, frontImage))
          (root, proof :: _) = CredentialBatches.batch(List(credential))
          credentialResponse <- EitherT[Task, PrismError, IssueCredentialBatchResponse](
            nodeService
              .issueCredentialBatch(root)
              .redeem(
                ex => Left(CannotIssueCredentialBatch(ex.getMessage)),
                Right(_)
              )
          )

          // send credential along with inclusion proof
          credentialProto = credential_models.PlainTextCredential(
            encodedCredential = credential.canonicalForm,
            encodedMerkleProof = proof.encode
          )
          atalaMessage = AtalaMessage().withPlainCredential(credentialProto)

          _ = logger.info(
            s"Credential batch created for document instance id: ${message.documentInstanceId} with batch id: ${credentialResponse.batchId}"
          )
        } yield Some(atalaMessage)).value
      }
  }

  private[processors] def parseAcuantProcessFinishedMessage(
      message: ReceivedMessage
  ): Option[AcuantProcessFinished] = {
    Try(AtalaMessage.parseFrom(message.message.toByteArray)).toOption
      .flatMap(_.message.kycBridgeMessage)
      .flatMap(_.message.acuantProcessFinished)
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
          CredentialContent.JsonFields.IssuanceKeyId.field -> authConfig.didKeyId,
          CredentialContent.JsonFields.CredentialSubject.field -> credentialSubject
        )
      )
      .sign(authConfig.didKeyPair.privateKey)
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

object DocumentUploadedMessageProcessor {
  case class FaceMatchFailedError(faceMatchResponse: FaceMatchResponse) extends PrismError {
    override def toStatus: Status = {
      Status.INVALID_ARGUMENT.withDescription(
        s"User's selfie doesn't match photo extracted from document, face id score ${faceMatchResponse.score}"
      )
    }
  }

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
