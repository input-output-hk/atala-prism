package io.iohk.atala.prism.console.services

import java.util.UUID

import io.iohk.atala.prism.connector.ConnectorAuthenticator
import io.iohk.atala.prism.models.{ProtoCodecs => CommonProtoCodecs}
import io.iohk.atala.prism.console.grpc.ProtoCodecs._
import io.iohk.atala.prism.console.models.{
  Contact,
  CreateGenericCredential,
  GenericCredential,
  Institution,
  PublishCredential
}
import io.iohk.atala.prism.console.repositories.{ContactsRepository, CredentialsRepository}
import io.iohk.atala.prism.credentials.SlayerCredentialId
import io.iohk.atala.prism.crypto.SHA256Digest
import io.iohk.atala.prism.identity.DID
import io.iohk.atala.prism.models.ProtoCodecs
import io.iohk.atala.prism.protos.cmanager_api._
import io.iohk.atala.prism.protos.node_api.NodeServiceGrpc
import io.iohk.atala.prism.protos.node_models.SignedAtalaOperation
import io.iohk.atala.prism.protos.{cmanager_api, node_api}
import io.iohk.atala.prism.utils.FutureEither
import io.iohk.atala.prism.utils.FutureEither.FutureOptionOps
import io.iohk.atala.prism.utils.syntax._
import io.scalaland.chimney.dsl._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class CredentialsServiceImpl(
    credentialsRepository: CredentialsRepository,
    contactsRepository: ContactsRepository,
    authenticator: ConnectorAuthenticator,
    nodeService: NodeServiceGrpc.NodeService
)(implicit
    ec: ExecutionContext
) extends cmanager_api.CredentialsServiceGrpc.CredentialsService {

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
            .map(_.contactId)

        case None =>
          val maybe = Try(request.contactId)
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
        issuanceOperationHash = SHA256Digest(request.operationHash.toByteArray.toVector)
        encodedSignedCredential = request.encodedSignedCredential
        signedIssueCredentialOp =
          request.issueCredentialOperation
            .getOrElse(throw new RuntimeException("Missing IssueCredential operation"))
        // validation for sanity check
        (contentHash, did, operationHash) = extractValues(signedIssueCredentialOp)
        slayerId = SlayerCredentialId.compute(contentHash, did)
        _ = require(
          credentialProtocolId == slayerId.string,
          "Invalid credential protocol id"
        )
        _ = require(
          issuanceOperationHash == operationHash,
          "Operation hash does not match the provided operation"
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
            .withSignedOperation(signedIssueCredentialOp)
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

  private def extractValues(signedAtalaOperation: SignedAtalaOperation): (SHA256Digest, DID, SHA256Digest) = {
    val maybePair = for {
      atalaOperation <- signedAtalaOperation.operation
      opHash = SHA256Digest.compute(atalaOperation.toByteArray)
      issueCredential <- atalaOperation.operation.issueCredential
      credentialData <- issueCredential.credentialData
      did = DID.buildPrismDID(credentialData.issuer)
      contentHash = SHA256Digest(credentialData.contentHash.toByteArray.toVector)
    } yield (contentHash, did, opHash)
    maybePair.getOrElse(throw new RuntimeException("Failed to extract content hash and issuer DID"))
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

  override def getContactCredentials(request: GetContactCredentialsRequest): Future[GetContactCredentialsResponse] = {
    def f(institutionId: Institution.Id): Future[GetContactCredentialsResponse] = {
      val contactIdF = Future.fromTry {
        Try {
          Contact.Id(UUID.fromString(request.contactId))
        }
      }

      for {
        contactId <- contactIdF
        response <-
          credentialsRepository
            .getBy(institutionId, contactId)
            .map { list =>
              cmanager_api.GetContactCredentialsResponse(list.map(genericCredentialToProto))
            }
            .value
            .map {
              case Right(x) => x
              case Left(e) => throw new RuntimeException(s"FAILED: $e")
            }
      } yield response
    }

    authenticator.authenticated("getSubjectCredentials", request) { participantId =>
      f(Institution.Id(participantId.uuid))
    }
  }

  override def shareCredential(request: ShareCredentialRequest): Future[ShareCredentialResponse] = {
    def f(institutionId: Institution.Id): Future[ShareCredentialResponse] = {
      val credentialIdF = Future.fromTry {
        Try {
          GenericCredential.Id(UUID.fromString(request.cmanagerCredentialId))
        }
      }

      for {
        credentialId <- credentialIdF
        response <-
          credentialsRepository
            .markAsShared(institutionId, credentialId)
            .map { _ =>
              cmanager_api.ShareCredentialResponse()
            }
            .value
            .map {
              case Right(x) => x
              case Left(e) => throw new RuntimeException(s"FAILED: $e")
            }
      } yield response
    }

    authenticator.authenticated("shareCredential", request) { participantId =>
      f(Institution.Id(participantId.uuid))
    }
  }

  /** Retrieves node information associated to a credential
    */
  override def getBlockchainData(request: GetBlockchainDataRequest): Future[GetBlockchainDataResponse] = {
    authenticator.authenticated("getBlockchainData", request) { _ =>
      // TODO: The node currently does not store the transaction data in a useful way.
      //       Hence, we will have this workaround: We will return the data from the
      //       published_credentials table to unlock the flow.
      //       This must be updated after we update the node
      credentialsRepository
        .getTransactionInfo(request.encodedSignedCredential)
        .map { maybeTransactionInfo =>
          maybeTransactionInfo.fold(GetBlockchainDataResponse())(txInfo =>
            GetBlockchainDataResponse().withIssuanceProof(CommonProtoCodecs.toTransactionInfo(txInfo))
          )
        }
        .value
        .map {
          case Right(x) => x
          case Left(e) => throw new RuntimeException(s"FAILED: $e")
        }
    }
  }
}
