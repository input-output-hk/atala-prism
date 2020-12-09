package io.iohk.atala.prism.console.services

import java.util.UUID

import io.iohk.atala.prism.connector.ConnectorAuthenticator
import io.iohk.atala.prism.connector.errors.ConnectorErrorSupport
import io.iohk.atala.prism.connector.model.ConnectionId
import io.iohk.atala.prism.console.models.{Contact, CredentialExternalId, Institution}
import io.iohk.atala.prism.console.repositories.daos.StoredCredentialsDAO.StoredSignedCredentialData
import io.iohk.atala.prism.console.repositories.StoredCredentialsRepository
import io.iohk.atala.prism.errors.LoggingContext
import io.iohk.atala.prism.models.ParticipantId
import io.iohk.atala.prism.protos.cstore_api.{
  GetLatestCredentialExternalIdRequest,
  GetLatestCredentialExternalIdResponse
}
import io.iohk.atala.prism.protos.{cstore_api, cstore_models}
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class CredentialsStoreService(
    storedCredentials: StoredCredentialsRepository,
    authenticator: ConnectorAuthenticator
)(implicit
    ec: ExecutionContext
) extends cstore_api.CredentialsStoreServiceGrpc.CredentialsStoreService
    with ConnectorErrorSupport {

  override val logger: Logger = LoggerFactory.getLogger(this.getClass)

  override def storeCredential(
      request: cstore_api.StoreCredentialRequest
  ): Future[cstore_api.StoreCredentialResponse] = {
    def f(participantId: ParticipantId) = {
      implicit val loggingContext = LoggingContext("request" -> request, "userId" -> participantId)

      val credentialExternalIdF = Future.fromTry {
        Try { CredentialExternalId(request.credentialExternalId) }
      }

      for {
        credentialExternalId <- credentialExternalIdF
        createData = StoredSignedCredentialData(
          connectionId = ConnectionId.apply(request.connectionId),
          encodedSignedCredential = request.encodedSignedCredential,
          credentialExternalId = credentialExternalId
        )
        response <-
          storedCredentials
            .storeCredential(createData)
            .wrapExceptions
            .successMap { _ =>
              cstore_api.StoreCredentialResponse()
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
          cstore_api.GetLatestCredentialExternalIdResponse(
            latestCredentialExternalId = maybeCredentialExternalId.fold("")(_.value.toString)
          )
        }
    }

    authenticator.authenticated("getLastStoredMessageId", request) { participantId =>
      f(Institution.Id(participantId.uuid))
    }
  }

  override def getStoredCredentialsFor(
      request: cstore_api.GetStoredCredentialsForRequest
  ): Future[cstore_api.GetStoredCredentialsForResponse] = {
    def f(institutionId: Institution.Id) = {
      implicit val loggingContext = LoggingContext("request" -> request, "userId" -> institutionId)

      for {
        maybeContactId <- Future.fromTry(
          Try {
            if (request.individualId.isEmpty) None
            else Some(Contact.Id(UUID.fromString(request.individualId)))
          }
        )
        response <-
          storedCredentials
            .getCredentialsFor(institutionId, maybeContactId)
            .wrapExceptions
            .successMap { credentials =>
              cstore_api.GetStoredCredentialsForResponse(
                credentials = credentials.map { credential =>
                  cstore_models.StoredSignedCredential(
                    individualId = credential.individualId.value.toString,
                    encodedSignedCredential = credential.encodedSignedCredential,
                    storedAt = credential.storedAt.toEpochMilli
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
