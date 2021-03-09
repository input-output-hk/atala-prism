package io.iohk.atala.prism.management.console.services

import com.google.protobuf.ByteString
import io.iohk.atala.prism.auth.AuthSupport
import io.iohk.atala.prism.errors.LoggingContext
import io.iohk.atala.prism.grpc.ProtoConverter
import io.iohk.atala.prism.management.console.ManagementConsoleAuthenticator
import io.iohk.atala.prism.management.console.errors.{ManagementConsoleError, ManagementConsoleErrorSupport}
import io.iohk.atala.prism.management.console.grpc._
import io.iohk.atala.prism.management.console.integrations.ParticipantsIntegrationService
import io.iohk.atala.prism.management.console.models.{GetStatistics, ParticipantId, RegisterDID}
import io.iohk.atala.prism.management.console.repositories.StatisticsRepository
import io.iohk.atala.prism.models.{ProtoCodecs => CommonProtoCodecs}
import io.iohk.atala.prism.protos.common_models.{HealthCheckRequest, HealthCheckResponse}
import io.iohk.atala.prism.protos.console_api
import io.iohk.atala.prism.protos.console_api._
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

class ConsoleServiceImpl(
    participantsIntegrationService: ParticipantsIntegrationService,
    statisticsRepository: StatisticsRepository,
    val authenticator: ManagementConsoleAuthenticator
)(implicit
    ec: ExecutionContext
) extends console_api.ConsoleServiceGrpc.ConsoleService
    with ManagementConsoleErrorSupport
    with AuthSupport[ManagementConsoleError, ParticipantId] {

  val logger: Logger = LoggerFactory.getLogger(this.getClass)

  override def healthCheck(request: HealthCheckRequest): Future[HealthCheckResponse] =
    Future.successful(HealthCheckResponse())

  override def getStatistics(request: GetStatisticsRequest): Future[GetStatisticsResponse] =
    auth[GetStatistics]("getStatistics", request) { (participantId, getStatistics) =>
      statisticsRepository
        .query(participantId, getStatistics.timeInterval)
        .map(ProtoCodecs.toStatisticsProto)
    }

  override def registerDID(request: RegisterConsoleDIDRequest): Future[RegisterConsoleDIDResponse] = {
    implicit val codec = implicitly[ProtoConverter[RegisterConsoleDIDRequest, RegisterDID]]
    codec.fromProto(request) match {
      case Failure(exception) =>
        val response = invalidRequest(exception.getMessage)
        respondWith(request, response)

      case Success(query) =>
        // Assemble LoggingContext out of the case class fields
        implicit val lc: LoggingContext = LoggingContext(
          (0 until query.productArity)
            .map(i => query.productElementName(i) -> query.productElement(i).toString)
            .toMap
        )
        participantsIntegrationService
          .register(query)
          .map { data =>
            RegisterConsoleDIDResponse()
              .withDid(data.did.value)
              .withTransactionInfo(CommonProtoCodecs.toTransactionInfo(data.transactionInfo))
          }
          .wrapExceptions
          .flatten
    }
  }

  override def getCurrentUser(request: GetConsoleCurrentUserRequest): Future[GetConsoleCurrentUserResponse] = {
    unitAuth("getCurrentUser", request) { (participantId, _) =>
      participantsIntegrationService
        .getDetails(participantId)
        .map { info =>
          val logoBytes = info.logo.map(_.bytes.toArray).getOrElse(Array.empty)
          GetConsoleCurrentUserResponse()
            .withName(info.name)
            .withLogo(ByteString.copyFrom(logoBytes))
        }
    }
  }
}
