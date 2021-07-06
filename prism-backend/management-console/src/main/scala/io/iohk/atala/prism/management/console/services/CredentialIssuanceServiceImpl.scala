package io.iohk.atala.prism.management.console.services

import cats.effect.IO
import cats.implicits.catsSyntaxEitherId
import cats.syntax.option._
import io.iohk.atala.prism.auth.AuthAndMiddlewareSupport
import io.iohk.atala.prism.management.console.ManagementConsoleAuthenticator
import io.iohk.atala.prism.management.console.errors.{ManagementConsoleError, ManagementConsoleErrorSupport}
import io.iohk.atala.prism.management.console.grpc._
import io.iohk.atala.prism.management.console.models.ParticipantId
import io.iohk.atala.prism.management.console.repositories.CredentialIssuancesRepository
import io.iohk.atala.prism.management.console.repositories.CredentialIssuancesRepository._
import io.iohk.atala.prism.protos.console_api
import io.iohk.atala.prism.protos.console_api._
import io.iohk.atala.prism.protos.console_models.CredentialIssuanceContact
import io.iohk.atala.prism.utils.FutureEither.FutureEitherOps
import io.iohk.atala.prism.utils.syntax._
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.{ExecutionContext, Future}

class CredentialIssuanceServiceImpl(
    credentialIssuancesRepository: CredentialIssuancesRepository[IO],
    val authenticator: ManagementConsoleAuthenticator
)(implicit
    ec: ExecutionContext
) extends console_api.CredentialIssuanceServiceGrpc.CredentialIssuanceService
    with ManagementConsoleErrorSupport
    with AuthAndMiddlewareSupport[ManagementConsoleError, ParticipantId] {

  override protected val serviceName: String = "credential-issuance-service"

  val logger: Logger = LoggerFactory.getLogger(this.getClass)

  override def createCredentialIssuance(
      request: CreateCredentialIssuanceRequest
  ): Future[CreateCredentialIssuanceResponse] =
    auth[CreateCredentialIssuance]("createCredentialIssuance", request) { (participantId, query) =>
      credentialIssuancesRepository
        .create(participantId, query)
        .unsafeToFuture()
        .toFutureEither
        .map { credentialIssuanceId =>
          CreateCredentialIssuanceResponse(credentialIssuanceId = credentialIssuanceId.toString)
        }
    }

  override def getCredentialIssuance(
      request: GetCredentialIssuanceRequest
  ): Future[GetCredentialIssuanceResponse] =
    auth[GetCredentialIssuance]("getCredentialIssuance", request) { (participantId, query) =>
      credentialIssuancesRepository
        .get(query.credentialIssuanceId, participantId)
        .map { credentialIssuance =>
          GetCredentialIssuanceResponse(
            name = credentialIssuance.name,
            credentialTypeId = credentialIssuance.credentialTypeId.uuid.toString,
            createdAt = credentialIssuance.createdAt.toProtoTimestamp.some,
            credentialIssuanceContacts = credentialIssuance.contacts.map(contact =>
              CredentialIssuanceContact(
                contactId = contact.contactId.toString,
                credentialData = contact.credentialData.noSpaces,
                groupIds = contact.groupIds.map(_.toString)
              )
            )
          )
        }
        .unsafeToFuture()
        .map(_.asRight)
        .toFutureEither
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
        .unsafeToFuture()
        .toFutureEither
        .map { credentialIssuanceId =>
          CreateGenericCredentialBulkResponse(credentialIssuanceId.uuid.toString)
        }
    }
}
