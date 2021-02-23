package io.iohk.atala.prism.management.console.services

import io.iohk.atala.prism.auth.AuthSupport
import io.iohk.atala.prism.management.console.ManagementConsoleAuthenticator
import io.iohk.atala.prism.management.console.errors.{ManagementConsoleError, ManagementConsoleErrorSupport}
import io.iohk.atala.prism.management.console.grpc.{ProtoCodecs, getStatisticsConverter}
import io.iohk.atala.prism.management.console.models.{GetStatistics, ParticipantId}
import io.iohk.atala.prism.management.console.repositories.StatisticsRepository
import io.iohk.atala.prism.protos.common_models.{HealthCheckRequest, HealthCheckResponse}
import io.iohk.atala.prism.protos.console_api
import io.iohk.atala.prism.protos.console_api._
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.{ExecutionContext, Future}

class ConsoleServiceImpl(statisticsRepository: StatisticsRepository, val authenticator: ManagementConsoleAuthenticator)(
    implicit ec: ExecutionContext
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
}
