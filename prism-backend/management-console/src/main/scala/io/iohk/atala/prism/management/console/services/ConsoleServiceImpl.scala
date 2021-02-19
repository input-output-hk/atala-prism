package io.iohk.atala.prism.management.console.services

import io.iohk.atala.prism.errors.LoggingContext
import io.iohk.atala.prism.management.console.ManagementConsoleAuthenticator
import io.iohk.atala.prism.management.console.errors.{GetStatisticsInvalidRequest, ManagementConsoleErrorSupport}
import io.iohk.atala.prism.management.console.grpc.ProtoCodecs
import io.iohk.atala.prism.management.console.models.{GetStatistics, ParticipantId}
import io.iohk.atala.prism.management.console.repositories.StatisticsRepository
import io.iohk.atala.prism.protos.common_models.{HealthCheckRequest, HealthCheckResponse}
import io.iohk.atala.prism.protos.console_api
import io.iohk.atala.prism.protos.console_api._
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

class ConsoleServiceImpl(statisticsRepository: StatisticsRepository, authenticator: ManagementConsoleAuthenticator)(
    implicit ec: ExecutionContext
) extends console_api.ConsoleServiceGrpc.ConsoleService
    with ManagementConsoleErrorSupport {

  val logger: Logger = LoggerFactory.getLogger(this.getClass)

  override def healthCheck(request: HealthCheckRequest): Future[HealthCheckResponse] =
    Future.successful(HealthCheckResponse())

  override def getStatistics(request: GetStatisticsRequest): Future[GetStatisticsResponse] = {
    def f(participantId: ParticipantId, getStatistics: GetStatistics): Future[GetStatisticsResponse] = {
      implicit val loggingContext: LoggingContext =
        LoggingContext("request" -> request, "participantId" -> participantId)

      for {
        response <-
          statisticsRepository
            .query(participantId, getStatistics.timeInterval)
            .map(ProtoCodecs.toStatisticsProto)
            .wrapExceptions
            .flatten
      } yield response
    }

    authenticator.authenticated("getStatistics", request) { participantId =>
      ProtoCodecs.toGetStatistics(request) match {
        case Failure(exception) =>
          val response = GetStatisticsInvalidRequest(exception.getMessage)
          respondWith(request, response)
        case Success(getStatistics) =>
          f(participantId, getStatistics)
      }
    }
  }
}
