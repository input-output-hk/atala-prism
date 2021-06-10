package io.iohk.atala.prism.console.services

import io.iohk.atala.prism.auth.AuthAndMiddlewareSupport
import io.iohk.atala.prism.connector.ConnectorAuthenticator
import io.iohk.atala.prism.connector.errors.{ConnectorError, ConnectorErrorSupport}
import io.iohk.atala.prism.console.models.Institution
import io.iohk.atala.prism.console.repositories.StatisticsRepository
import io.iohk.atala.prism.models.ParticipantId
import io.iohk.atala.prism.protos.common_models.{HealthCheckRequest, HealthCheckResponse}
import io.iohk.atala.prism.protos.console_api
import io.iohk.atala.prism.protos.console_api._
import io.scalaland.chimney.dsl._
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.{ExecutionContext, Future}

class ConsoleServiceImpl(statisticsRepository: StatisticsRepository, val authenticator: ConnectorAuthenticator)(implicit
    ec: ExecutionContext
) extends console_api.ConsoleServiceGrpc.ConsoleService
    with ConnectorErrorSupport
    with AuthAndMiddlewareSupport[ConnectorError, ParticipantId] {

  override protected val serviceName: String = "console-service"

  val logger: Logger = LoggerFactory.getLogger(this.getClass)

  override def healthCheck(request: HealthCheckRequest): Future[HealthCheckResponse] =
    Future.successful(HealthCheckResponse())

  override def getStatistics(request: GetStatisticsRequest): Future[GetStatisticsResponse] =
    unitAuth("getStatistics", request) { (participantId, _) =>
      statisticsRepository
        .query(Institution.Id(participantId.uuid))
        .map { stats =>
          stats
            .into[GetStatisticsResponse]
            .withFieldConst(_.numberOfCredentialsInDraft, stats.numberOfCredentialsInDraft)
            .transform
        }
    }

  // Used only on the console backend
  override def registerDID(request: RegisterConsoleDIDRequest): Future[RegisterConsoleDIDResponse] = {
    Future.failed(throw new NotImplementedError("This is available on the console backend only"))
  }

  // Used only on the console backend
  override def getCurrentUser(request: GetConsoleCurrentUserRequest): Future[GetConsoleCurrentUserResponse] = {
    Future.failed(throw new NotImplementedError("This is available on the console backend only"))
  }

  // Used only on the console backend
  override def updateParticipantProfile(request: ConsoleUpdateProfileRequest): Future[ConsoleUpdateProfileResponse] =
    Future.failed(throw new NotImplementedError("This is available on the console backend only"))

}
