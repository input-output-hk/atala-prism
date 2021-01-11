package io.iohk.atala.prism.kycbridge.processors

import scala.util.Try

import monix.eval.Task
import cats.data.EitherT
import doobie.util.transactor.Transactor
import io.circe.syntax._

import io.iohk.atala.prism.services.{ConnectorClientService, MessageProcessor, NodeClientService}
import io.iohk.atala.prism.protos.connector_models.ReceivedMessage
import io.iohk.atala.prism.protos.credential_models
import io.iohk.atala.prism.protos.credential_models.{AtalaMessage, AcuantProcessFinished}
import io.iohk.atala.prism.protos.connector_api.SendMessageRequest
import io.iohk.atala.prism.kycbridge.db.ConnectionDao
import io.iohk.atala.prism.kycbridge.models.assureId.Document
import io.iohk.atala.prism.kycbridge.services.AssureIdService
import io.iohk.atala.prism.credentials.Credential
import io.iohk.atala.prism.credentials.content.CredentialContent
import io.iohk.atala.prism.credentials.content.syntax._
import io.iohk.atala.prism.config.ConnectorConfig
import io.iohk.atala.prism.crypto.ECTrait
import io.iohk.atala.prism.services.MessageProcessor.MessageProcessorException
import io.iohk.atala.prism.kycbridge.models.Connection

import doobie.implicits._
import io.iohk.atala.prism.kycbridge.models.assureId.implicits._

class DocumentUploadedMessageProcessor(
    tx: Transactor[Task],
    nodeService: NodeClientService,
    connectorService: ConnectorClientService,
    assureIdService: AssureIdService,
    connectorConfig: ConnectorConfig
)(implicit ec: ECTrait) {

  val processor: MessageProcessor = { receivedMessage =>
    parseAcuantProcessFinishedMessage(receivedMessage)
      .map { message =>
        (for {
          // get required informations
          connection <- EitherT(Connection.fromReceivedMessage(receivedMessage).transact(tx))
          documentStatus <- EitherT(assureIdService.getDocumentStatus(message.documentInstanceId)).leftMap(e =>
            MessageProcessorException(e.getMessage)
          )
          document <- EitherT(assureIdService.getDocument(message.documentInstanceId)).leftMap(e =>
            MessageProcessorException(e.getMessage)
          )

          // update connection with new document status
          _ <- EitherT.right[MessageProcessorException](
            ConnectionDao.update(connection.copy(acuantDocumentStatus = Some(documentStatus))).transact(tx)
          )

          // create credential with the document
          connectionId <- EitherT.fromOption[Task](connection.id, MessageProcessorException("Empty connection id."))
          credential = createCredential(document)
          _ <- EitherT.right[MessageProcessorException](
            nodeService.issueCredential(credential.canonicalForm)
          )

          // send credential
          credentialMessage = credential_models.Credential(
            typeId = "VerifiableCredential/AssureIdCredential",
            credentialDocument = credential.canonicalForm
          )
          sendMessageRequest =
            SendMessageRequest(connectionId = connectionId.uuid.toString, message = credentialMessage.toByteString)

          _ <- EitherT.right[MessageProcessorException](connectorService.sendMessage(sendMessageRequest))
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
          CredentialContent.JsonFields.IssuerDid.field -> connectorConfig.authConfig.did.value,
          CredentialContent.JsonFields.IssuanceKeyId.field -> connectorConfig.authConfig.didKeyId,
          CredentialContent.JsonFields.CredentialSubject.field -> credentialSubject
          // CredentialContent.JsonFields.CredentialSubject.field -> (credentialSubject ++ biographic ++ classificationType)
        )
      )
      .sign(connectorConfig.authConfig.didKeyPair.privateKey)
  }
}
