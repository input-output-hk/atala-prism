package io.iohk.atala.prism.console.services

import cats.syntax.functor._
import cats.syntax.option._
import io.iohk.atala.prism.auth.AuthSupport
import io.iohk.atala.prism.connector.ConnectorAuthenticator
import io.iohk.atala.prism.connector.errors.{ConnectorError, ConnectorErrorSupport}
import io.iohk.atala.prism.console.grpc._
import io.iohk.atala.prism.console.models.actions.{GetStoredCredentialsForRequest, StoreCredentialRequest}
import io.iohk.atala.prism.console.models.Institution
import io.iohk.atala.prism.console.repositories.StoredCredentialsRepository
import io.iohk.atala.prism.console.repositories.daos.ReceivedCredentialsDAO.StoredReceivedSignedCredentialData
import io.iohk.atala.prism.models.ParticipantId
import io.iohk.atala.prism.protos.console_api.{
  GetLatestCredentialExternalIdRequest,
  GetLatestCredentialExternalIdResponse
}
import io.iohk.atala.prism.protos.{console_api, console_models}
import io.iohk.atala.prism.utils.syntax._
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.{ExecutionContext, Future}

class CredentialsStoreService(
    storedCredentials: StoredCredentialsRepository,
    val authenticator: ConnectorAuthenticator
)(implicit
    ec: ExecutionContext
) extends console_api.CredentialsStoreServiceGrpc.CredentialsStoreService
    with ConnectorErrorSupport
    with AuthSupport[ConnectorError, ParticipantId] {

  override val logger: Logger = LoggerFactory.getLogger(this.getClass)

  override def storeCredential(
      request: console_api.StoreCredentialRequest
  ): Future[console_api.StoreCredentialResponse] =
    auth[StoreCredentialRequest]("storeCredential", request) { (_, storeCredentialRequest) =>
      val createData = StoredReceivedSignedCredentialData(
        connectionId = storeCredentialRequest.connectionId,
        encodedSignedCredential = request.encodedSignedCredential,
        credentialExternalId = storeCredentialRequest.externalId,
        merkleInclusionProof = storeCredentialRequest.merkleProof
      )
      storedCredentials
        .storeCredential(createData)
        .as(console_api.StoreCredentialResponse())
    }

  override def getLatestCredentialExternalId(
      request: GetLatestCredentialExternalIdRequest
  ): Future[GetLatestCredentialExternalIdResponse] =
    unitAuth("getLastStoredMessageId", request) { (participantId, _) =>
      val institutionId = Institution.Id(participantId.uuid)
      storedCredentials
        .getLatestCredentialExternalId(institutionId)
        .map { maybeCredentialExternalId =>
          console_api.GetLatestCredentialExternalIdResponse(
            latestCredentialExternalId = maybeCredentialExternalId.fold("")(_.value)
          )
        }
    }

  override def getStoredCredentialsFor(
      request: console_api.GetStoredCredentialsForRequest
  ): Future[console_api.GetStoredCredentialsForResponse] =
    auth[GetStoredCredentialsForRequest]("getStoredCredentialsFor", request) {
      (participantId, getStoredCredentialsForRequest) =>
        val institutionId = Institution.Id(participantId.uuid)
        storedCredentials
          .getCredentialsFor(institutionId, getStoredCredentialsForRequest.maybeContactId)
          .map { credentials =>
            console_api.GetStoredCredentialsForResponse(
              credentials = credentials.map { credential =>
                console_models.StoredSignedCredential(
                  individualId = credential.individualId.toString,
                  encodedSignedCredential = credential.encodedSignedCredential,
                  storedAt = credential.storedAt.toProtoTimestamp.some,
                  externalId = credential.externalId.value,
                  batchInclusionProof = credential.merkleInclusionProof.encode
                )
              }
            )
          }
    }
}
