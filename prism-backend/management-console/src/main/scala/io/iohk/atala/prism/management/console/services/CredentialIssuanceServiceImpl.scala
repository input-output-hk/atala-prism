package io.iohk.atala.prism.management.console.services

import io.iohk.atala.prism.auth.AuthSupport
import io.iohk.atala.prism.management.console.ManagementConsoleAuthenticator
import io.iohk.atala.prism.management.console.errors.{ManagementConsoleError, ManagementConsoleErrorSupport}
import io.iohk.atala.prism.management.console.grpc._
import io.iohk.atala.prism.management.console.models.ParticipantId
import io.iohk.atala.prism.management.console.repositories.CredentialIssuancesRepository
import io.iohk.atala.prism.management.console.repositories.CredentialIssuancesRepository._
import io.iohk.atala.prism.protos.console_api
import io.iohk.atala.prism.protos.console_api._
import io.iohk.atala.prism.protos.console_models.CredentialIssuanceContact
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.{ExecutionContext, Future}

class CredentialIssuanceServiceImpl(
    credentialIssuancesRepository: CredentialIssuancesRepository,
    val authenticator: ManagementConsoleAuthenticator
)(implicit
    ec: ExecutionContext
) extends console_api.CredentialIssuanceServiceGrpc.CredentialIssuanceService
    with ManagementConsoleErrorSupport
    with AuthSupport[ManagementConsoleError, ParticipantId] {

  val logger: Logger = LoggerFactory.getLogger(this.getClass)

  override def createCredentialIssuance(
      request: CreateCredentialIssuanceRequest
  ): Future[CreateCredentialIssuanceResponse] =
    auth[CreateCredentialIssuance]("createCredentialIssuance", request) { (participantId, query) =>
      credentialIssuancesRepository
        .create(participantId, query)
        .map { credentialIssuanceId =>
          CreateCredentialIssuanceResponse(credentialIssuanceId = credentialIssuanceId.toString)
        }
    }

  override def getCredentialIssuance(
      request: GetCredentialIssuanceRequest
  ): Future[GetCredentialIssuanceResponse] =
    auth[GetCredentialIssuance]("getCredentialIssuance", request) { (participantId, query) =>
      credentialIssuancesRepository.get(query.credentialIssuanceId, participantId).map { credentialIssuance =>
        GetCredentialIssuanceResponse(
          name = credentialIssuance.name,
          credentialTypeId = credentialIssuance.credentialTypeId.uuid.toString,
          createdAt = credentialIssuance.createdAt.toEpochMilli,
          credentialIssuanceContacts = credentialIssuance.contacts.map(contact =>
            CredentialIssuanceContact(
              contactId = contact.contactId.toString,
              credentialData = contact.credentialData.noSpaces,
              groupIds = contact.groupIds.map(_.toString)
            )
          )
        )
      }
    }

  override def createGenericCredentialBulk(
      request: CreateGenericCredentialBulkRequest
  ): Future[CreateGenericCredentialBulkResponse] =
    auth[CreateCredentialBulk]("createGenericCredentialBulk", request) { (participantId, query) =>
      credentialIssuancesRepository
        .createBulk(
          participantId,
          query.credentialsType,
          query.issuanceName,
          query.drafts
        )
        .map { credentialIssuanceId =>
          CreateGenericCredentialBulkResponse(credentialIssuanceId.uuid.toString)
        }
    }
}
