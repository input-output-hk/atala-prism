package io.iohk.atala.prism.kycbridge.processors

import scala.util.Try
import monix.eval.Task
import cats.data.EitherT
import doobie.util.transactor.Transactor
import io.circe.syntax._
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
import doobie.implicits._
import io.grpc.Status
import io.iohk.atala.prism.errors.PrismError
import io.iohk.atala.prism.kycbridge.models.assureId.implicits._
import io.iohk.atala.prism.kycbridge.models.faceId.FaceMatchResponse
import io.iohk.atala.prism.kycbridge.processors.DocumentUploadedMessageProcessor.{
  CannotIssueCredentialBatch,
  FaceMatchFailedError
}
import io.iohk.atala.prism.protos.node_api.IssueCredentialBatchResponse
import io.iohk.atala.prism.utils.ConnectionUtils
import io.iohk.atala.prism.utils.syntax.DBConnectionOps
import org.slf4j.LoggerFactory

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
          credential = createCredential(document)
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

  private[processors] def createCredential(document: Document): Credential = {
    val credentialSubject = document.asJson.noSpaces

    Credential
      .fromCredentialContent(
        CredentialContent(
          CredentialContent.JsonFields.IssuerDid.field -> authConfig.did.value,
          CredentialContent.JsonFields.IssuanceKeyId.field -> authConfig.didKeyId,
          CredentialContent.JsonFields.CredentialSubject.field -> credentialSubject
        )
      )
      .sign(authConfig.didKeyPair.privateKey)
  }
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
}
