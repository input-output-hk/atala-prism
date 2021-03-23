package io.iohk.atala.prism.kycbridge.services

import cats.syntax.functor._
import doobie.util.transactor.Transactor
import fs2.Stream
import io.iohk.atala.prism.kycbridge.db.ConnectionDao
import io.iohk.atala.prism.models.{ConnectionId, ConnectionState, ConnectionToken}
import io.iohk.atala.prism.services.ConnectorClientService
import io.iohk.atala.prism.utils.syntax.DBConnectionOps
import monix.eval.Task
import org.slf4j.LoggerFactory
import doobie.implicits._
import scala.concurrent.duration.DurationInt
import io.iohk.atala.prism.kycbridge.models.Connection

class ConnectionService(tx: Transactor[Task], connectorService: ConnectorClientService) {

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
          .evalMap(connectionInfo => {
            val connection = Connection(
              token = ConnectionToken(connectionInfo.token),
              id = ConnectionId.from(connectionInfo.connectionId).toOption,
              state = ConnectionState.Connected,
              acuantDocumentInstanceId = None,
              acuantDocumentStatus = None
            )

            logger.info(s"Connection accepted: ${connection}")

            ConnectionDao
              .update(connection)
              .logSQLErrors("updating connection", logger)
              .transact(tx)
              .as(connection)
          })
          .drain
      )
  }

}
