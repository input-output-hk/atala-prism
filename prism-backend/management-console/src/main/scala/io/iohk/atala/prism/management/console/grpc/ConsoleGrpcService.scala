package io.iohk.atala.prism.management.console.grpc

import cats.implicits.catsSyntaxEitherId
import com.google.protobuf.ByteString
import cats.syntax.functor._
import io.iohk.atala.prism.auth.AuthAndMiddlewareSupport
import io.iohk.atala.prism.errors.LoggingContext
import io.iohk.atala.prism.grpc.ProtoConverter
import io.iohk.atala.prism.logging.TraceId
import io.iohk.atala.prism.logging.TraceId.IOWithTraceIdContext
import io.iohk.atala.prism.management.console.ManagementConsoleAuthenticator
import io.iohk.atala.prism.management.console.errors.{ManagementConsoleError, ManagementConsoleErrorSupport}
import io.iohk.atala.prism.management.console.models._
import io.iohk.atala.prism.management.console.services.ConsoleService
import io.iohk.atala.prism.metrics.RequestMeasureUtil.measureRequestFuture
import io.iohk.atala.prism.protos.common_models.{HealthCheckRequest, HealthCheckResponse}
import io.iohk.atala.prism.protos.console_api
import io.iohk.atala.prism.protos.console_api._
import io.iohk.atala.prism.utils.FutureEither.{FutureEitherFOps, FutureEitherOps}
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

class ConsoleGrpcService(
    consoleService: ConsoleService[IOWithTraceIdContext],
    val authenticator: ManagementConsoleAuthenticator
)(implicit
    ec: ExecutionContext
) extends console_api.ConsoleServiceGrpc.ConsoleService
    with ManagementConsoleErrorSupport
    with AuthAndMiddlewareSupport[ManagementConsoleError, ParticipantId] {

  override protected val serviceName: String = "console-service"

  val logger: Logger = LoggerFactory.getLogger(this.getClass)

  override def healthCheck(
      request: HealthCheckRequest
  ): Future[HealthCheckResponse] =
    measureRequestFuture(serviceName, "healthCheck")(
      Future.successful(HealthCheckResponse())
    )

  override def getStatistics(
      request: GetStatisticsRequest
  ): Future[GetStatisticsResponse] =
    auth[GetStatistics]("getStatistics", request) { (participantId, traceId, getStatistics) =>
      consoleService
        .getStatistics(participantId, getStatistics)
        .run(traceId)
        .unsafeToFuture()
        .map(stats => ProtoCodecs.toStatisticsProto(stats).asRight)
        .toFutureEither
    }

  override def registerDID(
      request: RegisterConsoleDIDRequest
  ): Future[RegisterConsoleDIDResponse] = {
    val methodName = "registerDID"
    val codec =
      implicitly[ProtoConverter[RegisterConsoleDIDRequest, RegisterDID]]
    codec.fromProto(request) match {
      case Failure(exception) =>
        val response = invalidRequest(exception.getMessage)
        respondWith(request, response, serviceName, methodName)

      case Success(query) =>
        // Assemble LoggingContext out of the case class fields
        implicit val lc: LoggingContext = LoggingContext(
          (0 until query.productArity)
            .map(i => query.productElementName(i) -> query.productElement(i).toString)
            .toMap
        )
        measureRequestFuture(serviceName, methodName)(
          consoleService
            .registerDID(query)
            .run(TraceId.generateYOLO)
            .unsafeToFuture()
            .toFutureEither
            .as(RegisterConsoleDIDResponse())
            .wrapAndRegisterExceptions(serviceName, methodName)
            .flatten
        )
    }
  }

  override def getCurrentUser(
      request: GetConsoleCurrentUserRequest
  ): Future[GetConsoleCurrentUserResponse] = {
    unitAuth("getCurrentUser", request) { (participantId, traceId, _) =>
      consoleService
        .getCurrentUser(participantId)
        .run(traceId)
        .unsafeToFuture()
        .toFutureEither
        .map { info =>
          val logoBytes = info.logo.map(_.bytes.toArray).getOrElse(Array.empty)
          GetConsoleCurrentUserResponse()
            .withName(info.name)
            .withLogo(ByteString.copyFrom(logoBytes))
        }
    }
  }

  override def updateParticipantProfile(
      request: ConsoleUpdateProfileRequest
  ): Future[ConsoleUpdateProfileResponse] = {
    auth[UpdateParticipantProfile]("updateParticipantProfile", request) { (participantId, traceId, _) =>
      val logo = ParticipantLogo(request.logo.toByteArray.toVector)
      val participantProfile =
        UpdateParticipantProfile(request.name, Option(logo))
      consoleService
        .updateParticipantProfile(participantId, participantProfile)
        .run(traceId)
        .unsafeToFuture()
        .lift
        .as(ConsoleUpdateProfileResponse())
    }
  }
}
