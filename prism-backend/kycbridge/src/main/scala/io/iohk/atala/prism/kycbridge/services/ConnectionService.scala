package io.iohk.atala.prism.kycbridge.services

import doobie.implicits._
import doobie.util.transactor.Transactor
import fs2.Stream
import io.circe.syntax._
import io.iohk.atala.prism.kycbridge.db.ConnectionDao
import io.iohk.atala.prism.kycbridge.models.Connection
import io.iohk.atala.prism.kycbridge.task.lease.system.KycBridgeProcessingTaskState
import io.iohk.atala.prism.kycbridge.task.lease.system.data.AcuantStartProcessForConnectionData
import io.iohk.atala.prism.models.{ConnectionId, ConnectionState, ConnectionToken}
import io.iohk.atala.prism.services.ConnectorClientService
import io.iohk.atala.prism.task.lease.system.{ProcessingTaskData, ProcessingTaskService}
import io.iohk.atala.prism.utils.syntax.DBConnectionOps
import monix.eval.Task
import org.slf4j.LoggerFactory

import java.time.Instant
import scala.concurrent.duration.DurationInt

class ConnectionService(
    tx: Transactor[Task],
    connectorService: ConnectorClientService,
    processingTaskService: ProcessingTaskService[KycBridgeProcessingTaskState]
) {

  private val logger = LoggerFactory.getLogger(classOf[ConnectionService])

  private val GET_CONNECTIONS_PAGINATED_LIMIT = 100
  private val GET_CONNECTIONS_PAGINATED_AWAKE_DELAY = 11.seconds

  val connectionUpdateStream: Stream[Task, Unit] = {
    Stream
      .eval(
        ConnectionDao.findLastSeenConnectionId
          .logSQLErrors("finding last seen connection id", logger)
          .transact(tx)
      )
      .flatMap(lastSeenConnectionId =>
        connectorService
          .getConnectionsPaginatedStream(
            lastSeenConnectionId,
            GET_CONNECTIONS_PAGINATED_LIMIT,
            GET_CONNECTIONS_PAGINATED_AWAKE_DELAY
          )
          .evalMap(connectionInfo =>
            {
              val connection = Connection(
                token = ConnectionToken(connectionInfo.token),
                id = ConnectionId.from(connectionInfo.connectionId).toOption,
                state = ConnectionState.Connected,
                acuantDocumentInstanceId = None,
                acuantDocumentStatus = None
              )

              logger.info(s"Connection accepted: ${connection}")
              processConnection(connection)
            }.onErrorHandle[Any] { error =>
              logger.error("Error handling stream message", error)
            }
          )
          .drain
      )
  }

  private def processConnection(connection: Connection): Task[Unit] = {
    for {
      _ <-
        ConnectionDao
          .update(connection)
          .logSQLErrors("updating connection", logger)
          .transact(tx)
      _ <- processingTaskService.create(
        ProcessingTaskData(AcuantStartProcessForConnectionData(connection.token).asJson),
        KycBridgeProcessingTaskState.AcuantStartProcessForConnection,
        scheduledTime = Instant.now()
      )
    } yield ()
  }

}
