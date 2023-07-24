package io.iohk.atala.prism.management.console.grpc

import cats.effect.unsafe.IORuntime
import cats.implicits.catsSyntaxOptionId
import io.iohk.atala.prism.auth.{AuthAndMiddlewareSupportF, AuthenticatorF}
import io.iohk.atala.prism.logging.TraceId.IOWithTraceIdContext
import io.iohk.atala.prism.management.console.errors.{ManagementConsoleError, ManagementConsoleErrorSupport}
import io.iohk.atala.prism.management.console.models._
import io.iohk.atala.prism.management.console.services.CredentialTypeCategoryService
import io.iohk.atala.prism.protos.console_api
import io.iohk.atala.prism.protos.console_api.{
  ArchiveCredentialTypeCategoryRequest,
  ArchiveCredentialTypeCategoryResponse,
  CreateCredentialTypeCategoryRequest,
  CreateCredentialTypeCategoryResponse,
  GetCredentialTypeCategoriesRequest,
  GetCredentialTypeCategoriesResponse,
  UnArchiveCredentialTypeCategoryRequest,
  UnArchiveCredentialTypeCategoryResponse
}
import io.iohk.atala.prism.utils.FutureEither.FutureEitherOps
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.{ExecutionContext, Future}

class CredentialTypeCategoryGrpcService(
    credentialTypeCategoryService: CredentialTypeCategoryService[IOWithTraceIdContext],
    val authenticator: AuthenticatorF[ParticipantId, IOWithTraceIdContext]
)(implicit ec: ExecutionContext, runtime: IORuntime)
    extends console_api.CredentialTypeCategoriesServiceGrpc.CredentialTypeCategoriesService
    with ManagementConsoleErrorSupport
    with AuthAndMiddlewareSupportF[ManagementConsoleError, ParticipantId] {

  override protected val serviceName: String = "credential-type-categories-service"
  override val IOruntime: IORuntime = runtime

  val logger: Logger = LoggerFactory.getLogger(this.getClass)

  /** * AUTHENTICATED
    *
    * Retrieves the available credentialTypeCategories on the authenticated issuer.
    */
  override def getCredentialTypeCategories(
      request: GetCredentialTypeCategoriesRequest
  ): Future[GetCredentialTypeCategoriesResponse] = {
    auth[GetCredentialTypeCategories]("getCredentialTypeCategories", request) { (participantId, _, traceId) =>
      credentialTypeCategoryService
        .getCredentialTypeCategories(participantId)
        .run(traceId)
        .unsafeToFuture()
        .toFutureEither
        .map(result =>
          GetCredentialTypeCategoriesResponse(
            result.map(ProtoCodecs.toCredentialTypeCategoryProto)
          )
        )
    }
  }

  /** * AUTHENTICATED
    *
    * Creates credential type category for authenticated issuer.
    */
  override def createCredentialTypeCategory(
      request: CreateCredentialTypeCategoryRequest
  ): Future[CreateCredentialTypeCategoryResponse] = {
    auth[CreateCredentialTypeCategory]("createCredentialTypeCategory", request) {
      (participantId, createCredentialTypeCategoryData, traceId) =>
        credentialTypeCategoryService
          .createCredentialTypeCategory(participantId, createCredentialTypeCategoryData)
          .run(traceId)
          .unsafeToFuture()
          .toFutureEither
          .map(result =>
            CreateCredentialTypeCategoryResponse(
              ProtoCodecs.toCredentialTypeCategoryProto(result).some
            )
          )
    }
  }

  /** * AUTHENTICATED
    *
    * Marks credential type category for authenticated issuer.
    */
  override def archiveCredentialTypeCategory(
      request: ArchiveCredentialTypeCategoryRequest
  ): Future[ArchiveCredentialTypeCategoryResponse] = {
    auth[ArchiveCredentialTypeCategory]("archiveCredentialTypeCategory", request) {
      (_, archiveCredentialTypeCategoryData, traceId) =>
        credentialTypeCategoryService
          .archiveCredentialTypeCategory(archiveCredentialTypeCategoryData.id)
          .run(traceId)
          .unsafeToFuture()
          .toFutureEither
          .map(result =>
            ArchiveCredentialTypeCategoryResponse(
              ProtoCodecs.toCredentialTypeCategoryProto(result).some
            )
          )
    }
  }

  /** * AUTHENTICATED
    *
    * Unarchives credential type category for authenticated issuer.
    */
  override def unArchiveCredentialTypeCategory(
      request: UnArchiveCredentialTypeCategoryRequest
  ): Future[UnArchiveCredentialTypeCategoryResponse] =
    auth[UnArchiveCredentialTypeCategory]("unArchiveCredentialTypeCategory", request) {
      (_, unArchiveCredentialTypeCategoryData, traceId) =>
        credentialTypeCategoryService
          .unArchiveCredentialTypeCategory(unArchiveCredentialTypeCategoryData.id)
          .run(traceId)
          .unsafeToFuture()
          .toFutureEither
          .map(result =>
            UnArchiveCredentialTypeCategoryResponse(
              ProtoCodecs.toCredentialTypeCategoryProto(result).some
            )
          )
    }
}
