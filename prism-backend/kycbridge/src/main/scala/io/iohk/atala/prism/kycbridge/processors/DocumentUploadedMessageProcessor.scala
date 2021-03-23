package io.iohk.atala.prism.kycbridge.processors

import scala.util.Try
import monix.eval.Task
import cats.data.EitherT
import doobie.util.transactor.Transactor
import io.circe.syntax._
import io.iohk.atala.prism.services.{BaseGrpcClientService, ConnectorClientService, MessageProcessor, NodeClientService}
import io.iohk.atala.prism.protos.connector_models.ReceivedMessage
import io.iohk.atala.prism.protos.credential_models
import io.iohk.atala.prism.protos.credential_models.{AcuantProcessFinished, AtalaMessage}
import io.iohk.atala.prism.protos.connector_api.SendMessageRequest
import io.iohk.atala.prism.kycbridge.db.ConnectionDao
import io.iohk.atala.prism.kycbridge.models.assureId.Document
import io.iohk.atala.prism.kycbridge.services.{AssureIdService, FaceIdService}
import io.iohk.atala.prism.credentials.{Credential, CredentialBatches}
import io.iohk.atala.prism.credentials.content.CredentialContent
import io.iohk.atala.prism.credentials.content.syntax._
import io.iohk.atala.prism.crypto.ECTrait
import io.iohk.atala.prism.services.MessageProcessor.MessageProcessorException
import io.iohk.atala.prism.kycbridge.models.{Connection, faceId}
import doobie.implicits._
import io.iohk.atala.prism.kycbridge.models.assureId.implicits._
import io.iohk.atala.prism.utils.syntax.DBConnectionOps
import org.slf4j.LoggerFactory

class DocumentUploadedMessageProcessor(
    tx: Transactor[Task],
    nodeService: NodeClientService,
    connectorService: ConnectorClientService,
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
          // get required informations
          connection <- EitherT(
            Connection
              .fromReceivedMessage(receivedMessage)
              .logSQLErrors("getting connection from received message", logger)
              .transact(tx)
          )
          documentStatus <- EitherT(assureIdService.getDocumentStatus(message.documentInstanceId))
            .leftMap(MessageProcessorException.apply)
          document <-
            EitherT(assureIdService.getDocument(message.documentInstanceId)).leftMap(MessageProcessorException.apply)

          // update connection with new document status
          _ <- EitherT.right[MessageProcessorException](
            ConnectionDao
              .update(connection.copy(acuantDocumentStatus = Some(documentStatus)))
              .logSQLErrors("updating connection", logger)
              .transact(tx)
          )

          frontScannedImage <- EitherT(assureIdService.getFrontImageFromDocument(document.instanceId)).leftMap(e =>
            MessageProcessorException(
              s"Cannot fetch image extracted from document from assured id service: ${e.getMessage}"
            )
          )

          faceMatchData = faceId.Data(frontScannedImage, message.selfieImage.toByteArray)

          faceMatchResult <- EitherT(faceIdService.faceMatch(faceMatchData)).leftMap(e =>
            MessageProcessorException(
              s"Cannot check if user's selfie and photo extracted from document match: ${e.getMessage}"
            )
          )

          _ <- EitherT.cond[Task](
            faceMatchResult.isMatch,
            (),
            MessageProcessorException(
              s"User's selfie doesn't match photo extracted from document, face id score ${faceMatchResult.score}"
            )
          )

          // create credential batch with the document
          connectionId <- EitherT.fromOption[Task](connection.id, MessageProcessorException("Empty connection id."))
          credential = createCredential(document)
          (root, proof :: _) = CredentialBatches.batch(List(credential))
          credentialResponse <- EitherT.right[MessageProcessorException](nodeService.issueCredentialBatch(root))

          // send credential along with inclusion proof
          credentialMessage = credential_models.PlainTextCredential(
            encodedCredential = credential.canonicalForm,
            encodedMerkleProof = proof.encode
          )
          sendMessageRequest =
            SendMessageRequest(connectionId = connectionId.uuid.toString, message = credentialMessage.toByteString)

          _ <- EitherT.right[MessageProcessorException](connectorService.sendMessage(sendMessageRequest))
          _ = logger.info(
            s"Credential batch created for document instance id: ${message.documentInstanceId} with batch id: ${credentialResponse.batchId}"
          )
        } yield ()).value
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
    // TODO: this code is commented out due to the fact, that we send credentialSubject as a string
    // val credentialSubject: CredentialContent.Fields = CredentialContent.Fields(
    //   "documentInstanceId" -> document.instanceId
    // )

    // val biographic = document.biographic
    //   .map(biographic =>
    //     CredentialContent.Fields(
    //       "biographic" -> CredentialContent.Fields(
    //         "age" -> biographic.age,
    //         "birthDate" -> biographic.birthDate,
    //         "expirationDate" -> biographic.expirationDate,
    //         "fullName" -> biographic.fullName,
    //         "gender" -> biographic.gender,
    //         "photo" -> biographic.photo
    //       )
    //     )
    //   )
    //   .getOrElse(Nil)

    // val classificationType = document.classification
    //   .map(classification =>
    //     CredentialContent.Fields(
    //       "classificationType" -> CredentialContent.Fields(
    //         "class" -> classification.`type`.`class`,
    //         "className" -> classification.`type`.className,
    //         "countryCode" -> classification.`type`.countryCode,
    //         "issue" -> classification.`type`.issue,
    //         "name" -> classification.`type`.name
    //       )
    //     )
    //   )
    //   .getOrElse(Nil)
    val credentialSubject = document.asJson.noSpaces

    Credential
      .fromCredentialContent(
        CredentialContent(
          CredentialContent.JsonFields.IssuerDid.field -> authConfig.did.value,
          CredentialContent.JsonFields.IssuanceKeyId.field -> authConfig.didKeyId,
          CredentialContent.JsonFields.CredentialSubject.field -> credentialSubject
          // CredentialContent.JsonFields.CredentialSubject.field -> (credentialSubject ++ biographic ++ classificationType)
        )
      )
      .sign(authConfig.didKeyPair.privateKey)
  }
}
