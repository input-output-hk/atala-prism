package io.iohk.atala.prism.console.services

import io.iohk.atala.prism.connector.ConnectorAuthenticator
import io.iohk.atala.prism.connector.errors.ConnectorErrorSupport
import io.iohk.atala.prism.console.models.Institution
import io.iohk.atala.prism.console.repositories.StatisticsRepository
import io.iohk.atala.prism.errors.LoggingContext
import io.iohk.atala.prism.protos.common_models.{HealthCheckRequest, HealthCheckResponse}
import io.iohk.atala.prism.protos.console_api
import io.iohk.atala.prism.protos.console_api._
import io.scalaland.chimney.dsl._
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.{ExecutionContext, Future}

class ConsoleServiceImpl(statisticsRepository: StatisticsRepository, authenticator: ConnectorAuthenticator)(implicit
    ec: ExecutionContext
) extends console_api.ConsoleServiceGrpc.ConsoleService
    with ConnectorErrorSupport {

  val logger: Logger = LoggerFactory.getLogger(this.getClass)

  override def healthCheck(request: HealthCheckRequest): Future[HealthCheckResponse] =
    Future.successful(HealthCheckResponse())

  override def getStatistics(request: GetStatisticsRequest): Future[GetStatisticsResponse] = {
    def f(institutionId: Institution.Id): Future[GetStatisticsResponse] = {
      implicit val loggingContext: LoggingContext =
        LoggingContext("request" -> request, "institutionId" -> institutionId)

      for {
        response <-
          statisticsRepository
            .query(institutionId)
            .map { stats =>
              stats
                .into[GetStatisticsResponse]
                .withFieldConst(_.numberOfCredentialsInDraft, stats.numberOfCredentialsInDraft)
                .transform
            }
            .wrapExceptions
            .flatten
      } yield response
    }

    authenticator.authenticated("getStatistics", request) { participantId =>
      f(Institution.Id(participantId.uuid))
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
