package io.iohk.atala.prism.kycbridge.task.lease.system.processors

import cats.data.EitherT
import doobie.implicits._
import doobie.util.transactor.Transactor
import io.circe.syntax._
import io.iohk.atala.prism.kycbridge.db.ConnectionDao
import io.iohk.atala.prism.kycbridge.models.Connection.AcuantDocumentInstanceId
import io.iohk.atala.prism.kycbridge.models.assureId.{Device, DeviceType}
import io.iohk.atala.prism.kycbridge.services.{AcasService, AssureIdService}
import io.iohk.atala.prism.kycbridge.task.lease.system.KycBridgeProcessingTaskState
import io.iohk.atala.prism.kycbridge.task.lease.system.data.AcuantStartProcessForConnectionData
import io.iohk.atala.prism.protos.credential_models.StartAcuantProcess
import io.iohk.atala.prism.services.ConnectorClientService
import io.iohk.atala.prism.services.ConnectorClientService.CannotSendConnectorMessage
import io.iohk.atala.prism.task.lease.system.ProcessingTaskProcessorOps._
import io.iohk.atala.prism.task.lease.system.{
  ProcessingTask,
  ProcessingTaskData,
  ProcessingTaskProcessor,
  ProcessingTaskResult
}
import io.iohk.atala.prism.utils.ConnectionUtils
import io.iohk.atala.prism.utils.syntax.DBConnectionOps
import monix.eval.Task
import org.slf4j.LoggerFactory

class AcuantStartProcessForConnectionStateProcessor(
    tx: Transactor[Task],
    assureIdService: AssureIdService,
    acasService: AcasService,
    connectorService: ConnectorClientService
) extends ProcessingTaskProcessor[KycBridgeProcessingTaskState] {

  val MAX_ATTEMPTS = 5
  val REATTEMPT_SECONDS = 1L

  private implicit val logger = LoggerFactory.getLogger(this.getClass)

  private val device = Device(
    `type` = DeviceType(
      manufacturer = "manufacturer",
      model = "model"
    )
  )

  override def process(
      processingTask: ProcessingTask[KycBridgeProcessingTaskState],
      workerNumber: Int
  ): Task[ProcessingTaskResult[KycBridgeProcessingTaskState]] = {
    (for {
      acuantData <-
        parseProcessingTaskData[AcuantStartProcessForConnectionData, KycBridgeProcessingTaskState](processingTask)

      attemptNumber = acuantData.attemptsNumber.getOrElse(0) + 1

      connection <- EitherT(
        ConnectionUtils
          .fromConnectionId(acuantData.connectionId, acuantData.receivedMessageId, ConnectionDao.findByConnectionId)
          .logSQLErrors("getting connection from received message", logger)
          .transact(tx)
          .mapErrorToDelayedReattempt(
            processingTask,
            ProcessingTaskData(acuantData.copy(attemptsNumber = Some(attemptNumber)).asJson),
            attemptNumber,
            MAX_ATTEMPTS,
            REATTEMPT_SECONDS,
            onFailure = { error =>
              connectorService
                .sendResponseMessage(error.toAtalaMessage, acuantData.receivedMessageId, acuantData.connectionId)
                .void
            }
          )
      )

      documentInstanceResponseBody <- EitherT(
        assureIdService
          .createNewDocumentInstance(device)
          .logErrorIfPresent
          .mapErrorToDelayedReattempt(
            processingTask,
            ProcessingTaskData(acuantData.copy(attemptsNumber = Some(attemptNumber)).asJson),
            attemptNumber,
            MAX_ATTEMPTS,
            REATTEMPT_SECONDS,
            onFailure = { error =>
              connectorService
                .sendResponseMessage(error.toAtalaMessage, acuantData.receivedMessageId, acuantData.connectionId)
                .void
            }
          )
      )

      tokenResponseBody <-
        EitherT(acasService.getAccessToken.logErrorIfPresent.mapErrorToProcessingTaskScheduled(processingTask))

      connectionId <-
        EitherT
          .fromOption[Task](connection.id, ())
          .leftMap[ProcessingTaskResult[KycBridgeProcessingTaskState]] { _ =>
            logger.error(s"Connection doesn't contain id: ${connection}, cannot start acuant process")
            ProcessingTaskResult.ProcessingTaskFinished
          }

      _ <- EitherT.liftF(
        connectorService
          .sendStartAcuantProcess(
            connectionId,
            StartAcuantProcess(documentInstanceResponseBody.documentId, tokenResponseBody.accessToken),
            Some(acuantData.receivedMessageId)
          )
          .redeem(
            ex => Left(CannotSendConnectorMessage(ex.getMessage)),
            Right(_)
          )
          .logErrorIfPresent
          .mapErrorToProcessingTaskScheduled(processingTask)
      )

      updatedConnection = connection.copy(
        acuantDocumentInstanceId = Some(AcuantDocumentInstanceId(documentInstanceResponseBody.documentId)),
        acuantDocumentStatus = None
      )

      _ <- EitherT.right[ProcessingTaskResult[KycBridgeProcessingTaskState]](
        ConnectionDao
          .update(updatedConnection)
          .logSQLErrors("updating connection", logger)
          .transact(tx)
      )
    } yield ProcessingTaskResult.ProcessingTaskFinished).value
      .map(_.merge)
  }
}
