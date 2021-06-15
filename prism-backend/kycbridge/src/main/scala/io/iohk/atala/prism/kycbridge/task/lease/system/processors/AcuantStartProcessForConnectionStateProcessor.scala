package io.iohk.atala.prism.kycbridge.task.lease.system.processors

import cats.data.EitherT
import doobie.implicits._
import doobie.util.transactor.Transactor
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
import io.iohk.atala.prism.task.lease.system.{ProcessingTask, ProcessingTaskProcessor, ProcessingTaskResult}
import io.iohk.atala.prism.utils.syntax.DBConnectionOps
import monix.eval.Task
import org.slf4j.LoggerFactory

class AcuantStartProcessForConnectionStateProcessor(
    tx: Transactor[Task],
    assureIdService: AssureIdService,
    acasService: AcasService,
    connectorService: ConnectorClientService
) extends ProcessingTaskProcessor[KycBridgeProcessingTaskState] {

  private implicit val logger = LoggerFactory.getLogger(this.getClass)

  private val device = Device(
    `type` = DeviceType(
      manufacturer = "manufacturer",
      model = "model"
    )
  )

  override def process(
      processingTask: ProcessingTask[KycBridgeProcessingTaskState]
  ): Task[ProcessingTaskResult[KycBridgeProcessingTaskState]] = {
    (for {
      acuantData <-
        parseProcessingTaskData[AcuantStartProcessForConnectionData, KycBridgeProcessingTaskState](processingTask)

      connection <- EitherT(ConnectionDao.findByConnectionToken(acuantData.connectionToken).transact(tx).map {
        case Some(connection) => Right(connection)
        case None =>
          logger.error(s"Cannot fetch connection: ${acuantData.connectionToken}. Deleting processing task")
          Left(ProcessingTaskResult.ProcessingTaskFinished[KycBridgeProcessingTaskState]())
      })

      documentInstanceResponseBody <- EitherT(
        assureIdService
          .createNewDocumentInstance(device)
          .logErrorIfPresent
          .mapErrorToProcessingTaskScheduled(processingTask)
      )

      tokenResponseBody <-
        EitherT(acasService.getAccessToken.logErrorIfPresent.mapErrorToProcessingTaskScheduled(processingTask))

      connectionId <-
        EitherT
          .fromOption[Task](connection.id, ())
          .leftMap[ProcessingTaskResult[KycBridgeProcessingTaskState]] { _ =>
            logger.error(s"Connection doesn't contain id: ${connection}, cannot start acuant process")
            ProcessingTaskResult.ProcessingTaskFinished()
          }

      _ <- EitherT.liftF(
        connectorService
          .sendStartAcuantProcess(
            connectionId,
            StartAcuantProcess(documentInstanceResponseBody.documentId, tokenResponseBody.accessToken)
          )
          .redeem(
            ex => Left(CannotSendConnectorMessage(ex.getMessage)),
            Right(_)
          )
          .logErrorIfPresent
          .mapErrorToProcessingTaskScheduled(processingTask)
      )

      updatedConnection = connection.copy(
        acuantDocumentInstanceId = Some(AcuantDocumentInstanceId(documentInstanceResponseBody.documentId))
      )

      _ <- EitherT.right[ProcessingTaskResult[KycBridgeProcessingTaskState]](
        ConnectionDao
          .update(updatedConnection)
          .logSQLErrors("updating connection", logger)
          .transact(tx)
      )
    } yield ProcessingTaskResult.ProcessingTaskFinished()).value
      .map(_.merge)
  }
}
