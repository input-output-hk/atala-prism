package io.iohk.atala.prism.cmanager.grpc.services

import java.util.UUID

import io.iohk.atala.prism.cmanager.grpc.services.codecs.ProtoCodecs._
import io.iohk.atala.prism.cmanager.models.requests.{
  CreateGenericCredential,
  CreateUniversityCredential,
  PublishCredential
}
import io.iohk.atala.prism.cmanager.models._
import io.iohk.atala.prism.cmanager.repositories.CredentialsRepository
import io.iohk.atala.prism.connector.Authenticator
import io.iohk.atala.prism.console.models.{Contact, Institution}
import io.iohk.atala.prism.console.repositories.ContactsRepository
import io.iohk.atala.prism.crypto.SHA256Digest
import io.iohk.atala.prism.models.ProtoCodecs
import io.iohk.atala.prism.utils.FutureEither
import io.iohk.atala.prism.utils.FutureEither.FutureOptionOps
import io.iohk.atala.prism.utils.syntax._
import io.iohk.prism.protos.cmanager_api._
import io.iohk.prism.protos.{cmanager_api, node_api}
import io.iohk.prism.protos.node_api.NodeServiceGrpc
import io.scalaland.chimney.dsl._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class CredentialsServiceImpl(
    credentialsRepository: CredentialsRepository,
    contactsRepository: ContactsRepository,
    authenticator: Authenticator,
    nodeService: NodeServiceGrpc.NodeService
)(implicit
    ec: ExecutionContext
) extends cmanager_api.CredentialsServiceGrpc.CredentialsService {

  override def createCredential(
      request: cmanager_api.CreateCredentialRequest
  ): Future[cmanager_api.CreateCredentialResponse] =
    authenticatedHandler("createCredential", request) { issuerId =>
      val studentId = Student.Id(UUID.fromString(request.studentId))
      val model = request
        .into[CreateUniversityCredential]
        .withFieldConst(_.issuedBy, Institution.Id(issuerId))
        .withFieldConst(_.studentId, studentId)
        .enableUnsafeOption
        .transform

      credentialsRepository
        .createUniversityCredential(model)
        .map(universityCredentialToProto)
        .map(cmanager_api.CreateCredentialResponse().withCredential)
    }

  override def getCredentials(
      request: cmanager_api.GetCredentialsRequest
  ): Future[cmanager_api.GetCredentialsResponse] =
    authenticatedHandler("getCredentials", request) { issuerId =>
      val lastSeenCredential =
        Try(UUID.fromString(request.lastSeenCredentialId)).map(UniversityCredential.Id.apply).toOption
      credentialsRepository
        .getUniversityCredentialsBy(Institution.Id(issuerId), request.limit, lastSeenCredential)
        .map { list =>
          cmanager_api.GetCredentialsResponse(list.map(universityCredentialToProto))
        }
    }

  /** Generic versions
    */
  override def createGenericCredential(
      request: CreateGenericCredentialRequest
  ): Future[CreateGenericCredentialResponse] =
    authenticatedHandler("createGenericCredential", request) { issuerId =>
      // get the subjectId from the externalId
      // TODO: Avoid doing this when we stop accepting the subjectId
      val subjectIdF = Option(request.externalId)
        .filter(_.nonEmpty)
        .map(Contact.ExternalId.apply) match {
        case Some(externalId) =>
          contactsRepository
            .find(Institution.Id(issuerId), externalId)
            .map(_.getOrElse(throw new RuntimeException("The given externalId doesn't exists")))
            .map(_.id)

        case None =>
          val maybe = Try(request.subjectId)
            .filter(_.nonEmpty)
            .map(UUID.fromString)
            .map(Contact.Id.apply)
            .toOption

          Future
            .successful(maybe)
            .toFutureEither(
              new RuntimeException("The contactId is required, if it was provided, it's an invalid value")
            )
      }

      lazy val json =
        io.circe.parser.parse(request.credentialData).getOrElse(throw new RuntimeException("Invalid json"))

      val result = for {
        contactId <- subjectIdF
        model =
          request
            .into[CreateGenericCredential]
            .withFieldConst(_.issuedBy, Institution.Id(issuerId))
            .withFieldConst(_.subjectId, contactId)
            .withFieldConst(_.credentialData, json)
            .enableUnsafeOption
            .transform

        created <-
          credentialsRepository
            .create(model)
            .map(genericCredentialToProto)
      } yield cmanager_api.CreateGenericCredentialResponse().withGenericCredential(created)

      result.failOnLeft(identity)
    }

  override def getGenericCredentials(
      request: GetGenericCredentialsRequest
  ): Future[GetGenericCredentialsResponse] =
    authenticatedHandler("getGenericCredentials", request) { issuerId =>
      val lastSeenCredential =
        Try(UUID.fromString(request.lastSeenCredentialId)).map(GenericCredential.Id.apply).toOption
      credentialsRepository
        .getBy(Institution.Id(issuerId), request.limit, lastSeenCredential)
        .map { list =>
          cmanager_api.GetGenericCredentialsResponse(list.map(genericCredentialToProto))
        }
    }

  /** Publish an encoded signed credential into the blockchain
    */
  override def publishCredential(request: PublishCredentialRequest): Future[PublishCredentialResponse] = {
    authenticator.authenticated("publishCredential", request) { participantId =>
      for {
        issuerId <- Institution.Id(participantId.uuid).tryF
        credentialId = GenericCredential.Id(UUID.fromString(request.cmanagerCredentialId))
        credentialProtocolId = request.nodeCredentialId
        issuanceOperationHash = SHA256Digest(request.operationHash.toByteArray)
        encodedSignedCredential = request.encodedSignedCredential
        issueCredentialOp =
          request.issueCredentialOperation
            .getOrElse(throw new RuntimeException("Missing IssueCredential operation"))
        // validation for sanity check
        _ = require(
          credentialProtocolId == issuanceOperationHash.hexValue,
          "operation hash and credential id don't match"
        )
        _ = require(encodedSignedCredential.nonEmpty, "Empty encoded credential")
        // Verify issuer
        maybeCredential <- credentialsRepository.getBy(credentialId).toFuture
        credential =
          maybeCredential.getOrElse(throw new RuntimeException(s"Credential with ID $credentialId does not exist"))
        _ = require(credential.issuedBy == issuerId, "The credential was not issued by the specified issuer")
        // Issue the credential in the Node
        credentialIssued <- nodeService.issueCredential {
          node_api
            .IssueCredentialRequest()
            .withSignedOperation(issueCredentialOp)
        }
        transactionInfo =
          credentialIssued.transactionInfo.getOrElse(throw new RuntimeException("Credential issues has no transaction"))
        // Update the database
        _ <-
          credentialsRepository
            .storePublicationData(
              issuerId,
              PublishCredential(
                credentialId,
                issuanceOperationHash,
                credentialProtocolId,
                encodedSignedCredential,
                ProtoCodecs.fromTransactionInfo(transactionInfo)
              )
            )
            .value
            .map {
              case Right(x) => x
              case Left(e) => throw new RuntimeException(s"FAILED: $e")
            }
      } yield cmanager_api.PublishCredentialResponse().withTransactionInfo(transactionInfo)
    }
  }

  private def authenticatedHandler[Request <: scalapb.GeneratedMessage, Response <: scalapb.GeneratedMessage](
      methodName: String,
      request: Request
  )(
      block: UUID => FutureEither[Nothing, Response]
  ): Future[Response] = {
    authenticator.authenticated(methodName, request) { participantId =>
      block(participantId.uuid).value
        .map {
          case Right(x) => x
          case Left(e) => throw new RuntimeException(s"FAILED: $e")
        }
    }
  }
}
