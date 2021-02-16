package io.iohk.atala.prism.console.services

import io.iohk.atala.prism.connector.ConnectorAuthenticator
import io.iohk.atala.prism.connector.errors.ConnectorErrorSupport
import io.iohk.atala.prism.connector.model.ConnectionId
import io.iohk.atala.prism.console.models.{Contact, CredentialExternalId, Institution}
import io.iohk.atala.prism.console.repositories.StoredCredentialsRepository
import io.iohk.atala.prism.console.repositories.daos.ReceivedCredentialsDAO.StoredReceivedSignedCredentialData
import io.iohk.atala.prism.crypto.MerkleTree.MerkleInclusionProof
import io.iohk.atala.prism.errors.LoggingContext
import io.iohk.atala.prism.models.ParticipantId
import io.iohk.atala.prism.protos.console_api.{
  GetLatestCredentialExternalIdRequest,
  GetLatestCredentialExternalIdResponse
}
import io.iohk.atala.prism.protos.{console_api, console_models}
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class CredentialsStoreService(
    storedCredentials: StoredCredentialsRepository,
    authenticator: ConnectorAuthenticator
)(implicit
    ec: ExecutionContext
) extends console_api.CredentialsStoreServiceGrpc.CredentialsStoreService
    with ConnectorErrorSupport {

  override val logger: Logger = LoggerFactory.getLogger(this.getClass)

  override def storeCredential(
      request: console_api.StoreCredentialRequest
  ): Future[console_api.StoreCredentialResponse] = {
    def f(participantId: ParticipantId) = {
      implicit val loggingContext = LoggingContext("request" -> request, "userId" -> participantId)

      val credentialExternalIdF = Future.fromTry {
        Try { CredentialExternalId(request.credentialExternalId) }
      }

      for {
        credentialExternalId <- credentialExternalIdF
        connectionId <- Future.fromTry(ConnectionId.from(request.connectionId))
        merkleProof <- Future.fromTry(
          Try {
            MerkleInclusionProof
              .decode(request.batchInclusionProof)
              .getOrElse(throw new RuntimeException(s"Failed to decode merkle proof: ${request.batchInclusionProof}"))
          }
        )
        createData = StoredReceivedSignedCredentialData(
          connectionId = connectionId,
          encodedSignedCredential = request.encodedSignedCredential,
          credentialExternalId = credentialExternalId,
          merkleInclusionProof = merkleProof
        )
        response <-
          storedCredentials
            .storeCredential(createData)
            .wrapExceptions
            .successMap { _ =>
              console_api.StoreCredentialResponse()
            }
      } yield response
    }

    authenticator.authenticated("storeCredential", request) { participantId =>
      f(participantId)
    }
  }

  override def getLatestCredentialExternalId(
      request: GetLatestCredentialExternalIdRequest
  ): Future[GetLatestCredentialExternalIdResponse] = {
    def f(institutionId: Institution.Id) = {
      implicit val loggingContext = LoggingContext("request" -> request, "userId" -> institutionId)
      storedCredentials
        .getLatestCredentialExternalId(institutionId)
        .wrapExceptions
        .successMap { maybeCredentialExternalId =>
          console_api.GetLatestCredentialExternalIdResponse(
            latestCredentialExternalId = maybeCredentialExternalId.fold("")(_.value.toString)
          )
        }
    }

    authenticator.authenticated("getLastStoredMessageId", request) { participantId =>
      f(Institution.Id(participantId.uuid))
    }
  }

  override def getStoredCredentialsFor(
      request: console_api.GetStoredCredentialsForRequest
  ): Future[console_api.GetStoredCredentialsForResponse] = {
    def f(institutionId: Institution.Id) = {
      implicit val loggingContext = LoggingContext("request" -> request, "userId" -> institutionId)

      val maybeContactIdF = if (request.individualId.nonEmpty) {
        Future.fromTry(Contact.Id.from(request.individualId)).map(Some(_))
      } else {
        Future.successful(None)
      }

      for {
        maybeContactId <- maybeContactIdF
        response <-
          storedCredentials
            .getCredentialsFor(institutionId, maybeContactId)
            .wrapExceptions
            .successMap { credentials =>
              console_api.GetStoredCredentialsForResponse(
                credentials = credentials.map { credential =>
                  console_models.StoredSignedCredential(
                    individualId = credential.individualId.toString,
                    encodedSignedCredential = credential.encodedSignedCredential,
                    storedAt = credential.storedAt.toEpochMilli,
                    externalId = credential.externalId.value,
                    batchInclusionProof = credential.merkleInclusionProof.encode
                  )
                }
              )
            }
      } yield response
    }

    authenticator.authenticated("getStoredCredentialsFor", request) { participantId =>
      f(Institution.Id(participantId.uuid))
    }
  }
}
