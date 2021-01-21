package io.iohk.atala.prism.management.console.services

import java.util.UUID

import io.iohk.atala.prism.management.console.models.{Contact, CredentialExternalId, ParticipantId}
import io.iohk.atala.prism.management.console.repositories.daos.ReceivedCredentialsDAO.ReceivedSignedCredentialData
import io.iohk.atala.prism.management.console.repositories.ReceivedCredentialsRepository
import io.iohk.atala.prism.errors.LoggingContext
import io.iohk.atala.prism.management.console.ManagementConsoleAuthenticator
import io.iohk.atala.prism.management.console.errors.ManagementConsoleErrorSupport
import io.iohk.atala.prism.protos.console_api.{
  GetLatestCredentialExternalIdRequest,
  GetLatestCredentialExternalIdResponse
}
import io.iohk.atala.prism.protos.{console_api, console_models}
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class CredentialsStoreServiceImpl(
    receivedCredentials: ReceivedCredentialsRepository,
    authenticator: ManagementConsoleAuthenticator
)(implicit
    ec: ExecutionContext
) extends console_api.CredentialsStoreServiceGrpc.CredentialsStoreService
    with ManagementConsoleErrorSupport {

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
        createData = ReceivedSignedCredentialData(
          contactId =
            Contact.Id(UUID.fromString(request.connectionId)), // TODO: Change proto model field name to contactId
          encodedSignedCredential = request.encodedSignedCredential,
          credentialExternalId = credentialExternalId
        )
        response <-
          receivedCredentials
            .createReceivedCredential(createData)
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
    def f(participantId: ParticipantId) = {
      implicit val loggingContext = LoggingContext("request" -> request, "userId" -> participantId)
      receivedCredentials
        .getLatestCredentialExternalId(participantId)
        .wrapExceptions
        .successMap { maybeCredentialExternalId =>
          console_api.GetLatestCredentialExternalIdResponse(
            latestCredentialExternalId = maybeCredentialExternalId.fold("")(_.value.toString)
          )
        }
    }

    authenticator.authenticated("getLastStoredMessageId", request) { participantId =>
      f(participantId)
    }
  }

  override def getStoredCredentialsFor(
      request: console_api.GetStoredCredentialsForRequest
  ): Future[console_api.GetStoredCredentialsForResponse] = {
    def f(participantId: ParticipantId) = {
      implicit val loggingContext = LoggingContext("request" -> request, "userId" -> participantId)

      for {
        contactId <- Future.fromTry(
          Try { Contact.Id(UUID.fromString(request.individualId)) }
        )
        response <-
          receivedCredentials
            .getCredentialsFor(participantId, contactId)
            .wrapExceptions
            .successMap { credentials =>
              console_api.GetStoredCredentialsForResponse(
                credentials = credentials.map { credential =>
                  console_models.StoredSignedCredential(
                    individualId = credential.individualId.value.toString,
                    encodedSignedCredential = credential.encodedSignedCredential,
                    storedAt = credential.receivedAt.toEpochMilli
                  )
                }
              )
            }
      } yield response
    }

    authenticator.authenticated("getStoredCredentialsFor", request) { participantId =>
      f(participantId)
    }
  }
}
