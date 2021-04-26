package io.iohk.atala.prism.kycbridge.services

import cats.data.EitherT
import doobie.util.transactor.Transactor
import fs2.Stream
import io.iohk.atala.prism.kycbridge.db.ConnectionDao
import monix.eval.Task
import doobie.implicits._
import io.iohk.atala.prism.kycbridge.models.Connection
import io.iohk.atala.prism.kycbridge.models.assureId.{Device, DeviceType}
import io.iohk.atala.prism.services.ConnectorClientService
import org.slf4j.LoggerFactory
import io.iohk.atala.prism.protos.credential_models.StartAcuantProcess
import io.iohk.atala.prism.kycbridge.models.Connection.AcuantDocumentInstanceId
import io.iohk.atala.prism.protos.connector_api.SendMessageResponse
import io.iohk.atala.prism.utils.syntax.DBConnectionOps

import scala.concurrent.duration.DurationInt

class AcuantService(
    tx: Transactor[Task],
    assureIdService: AssureIdService,
    acasService: AcasService,
    connectorService: ConnectorClientService
) {

  private val AWAKE_DELAY = 10.seconds

  private val logger = LoggerFactory.getLogger(this.getClass)

  private val device = Device(
    `type` = DeviceType(
      manufacturer = "manufacturer",
      model = "model"
    )
  )

  private def updateConnection(connection: Connection): Task[Either[Unit, Unit]] = {
    logger.info(s"Fetching document instance id and bearer token for connection: ${connection.token}")
    (for {
      documentInstanceResponseBody <- EitherT(assureIdService.createNewDocumentInstance(device)).leftMap(exception =>
        logger.warn(s"Acuant: Cannot create new document instance ${exception.getMessage}")
      )
      tokenResponseBody <- EitherT(acasService.getAccessToken).leftMap(exception =>
        logger.warn(s"Acuant: Cannot obtain bearer token ${exception.getMessage}")
      )

      connectionId <-
        EitherT
          .fromOption[Task](connection.id, ())
          .leftMap[Unit](_ => logger.warn(s"Connection doesn't contain id: ${connection}"))

      _ <- EitherT.liftF[Task, Unit, SendMessageResponse](
        connectorService.sendStartAcuantProcess(
          connectionId,
          StartAcuantProcess(documentInstanceResponseBody.documentId, tokenResponseBody.accessToken)
        )
      )

      updatedConnection = connection.copy(
        acuantDocumentInstanceId = Some(AcuantDocumentInstanceId(documentInstanceResponseBody.documentId))
      )

      _ <- EitherT.liftF[Task, Unit, Int](
        ConnectionDao
          .update(updatedConnection)
          .logSQLErrors("updating connection", logger)
          .transact(tx)
      )
    } yield ()).value
  }

  val acuantDataStream: Stream[Task, Unit] = {
    val initialAwakeDelay = false
    Stream
      .unfoldEval[Task, Boolean, Option[Connection]](initialAwakeDelay) { shouldApplyAwakeDelay =>
        for {
          _ <- if (shouldApplyAwakeDelay) Task.sleep(AWAKE_DELAY) else Task.unit
          connectionOption <-
            ConnectionDao.findConnectionWithoutDocumentId
              .logSQLErrors("finding connection without document id", logger)
              .transact(tx)
          _ <- connectionOption.fold(Task.unit)(connection => updateConnection(connection).void)
          applyAwakeDelay = connectionOption.isEmpty
        } yield Some(connectionOption -> applyAwakeDelay)
      }
      .drain
  }

}
