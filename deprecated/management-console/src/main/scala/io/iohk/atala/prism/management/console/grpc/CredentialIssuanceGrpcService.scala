package io.iohk.atala.prism.management.console.grpc

import cats.effect.unsafe.IORuntime
import cats.implicits.{catsSyntaxEitherId, catsSyntaxOptionId}
import io.iohk.atala.prism.auth.{AuthAndMiddlewareSupportF, AuthenticatorF}
import io.iohk.atala.prism.logging.TraceId.IOWithTraceIdContext
import io.iohk.atala.prism.management.console.errors._
import io.iohk.atala.prism.management.console.models._
import io.iohk.atala.prism.management.console.repositories.CredentialIssuancesRepository.{
  CreateCredentialBulk,
  CreateCredentialIssuance,
  GetCredentialIssuance
}
import io.iohk.atala.prism.management.console.services.CredentialIssuanceService
import io.iohk.atala.prism.protos.console_api
import io.iohk.atala.prism.protos.console_api._
import io.iohk.atala.prism.protos.console_models.CredentialIssuanceContact
import io.iohk.atala.prism.utils.FutureEither.FutureEitherOps
import io.iohk.atala.prism.utils.syntax.InstantToTimestampOps
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.{ExecutionContext, Future}

class CredentialIssuanceGrpcService(
    credentialIssuanceService: CredentialIssuanceService[IOWithTraceIdContext],
    val authenticator: AuthenticatorF[ParticipantId, IOWithTraceIdContext]
)(implicit
    ec: ExecutionContext,
    runtime: IORuntime
) extends console_api.CredentialIssuanceServiceGrpc.CredentialIssuanceService
    with ManagementConsoleErrorSupport
    with AuthAndMiddlewareSupportF[ManagementConsoleError, ParticipantId] {

  override protected val serviceName: String = "credential-issuance-service"
  override val IOruntime: IORuntime = runtime

  val logger: Logger = LoggerFactory.getLogger(this.getClass)

  override def createCredentialIssuance(
      request: CreateCredentialIssuanceRequest
  ): Future[CreateCredentialIssuanceResponse] =
    auth[CreateCredentialIssuance]("createCredentialIssuance", request) { (participantId, query, traceId) =>
      credentialIssuanceService
        .createCredentialIssuance(participantId, query)
        .run(traceId)
        .unsafeToFuture()
        .toFutureEither
        .map { credentialIssuanceId =>
          CreateCredentialIssuanceResponse(credentialIssuanceId = credentialIssuanceId.toString)
        }
    }

  override def getCredentialIssuance(
      request: GetCredentialIssuanceRequest
  ): Future[GetCredentialIssuanceResponse] =
    auth[GetCredentialIssuance]("getCredentialIssuance", request) { (participantId, query, traceId) =>
      credentialIssuanceService
        .getCredentialIssuance(participantId, query)
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
        .run(traceId)
        .unsafeToFuture()
        .map(_.asRight)
        .toFutureEither
    }

  override def createGenericCredentialBulk(
      request: CreateGenericCredentialBulkRequest
  ): Future[CreateGenericCredentialBulkResponse] =
    auth[CreateCredentialBulk]("createGenericCredentialBulk", request) { (participantId, query, traceId) =>
      credentialIssuanceService
        .createGenericCredentialBulk(participantId, query)
        .run(traceId)
        .unsafeToFuture()
        .toFutureEither
        .map { credentialIssuanceId =>
          CreateGenericCredentialBulkResponse(
            credentialIssuanceId.uuid.toString
          )
        }
    }
}
