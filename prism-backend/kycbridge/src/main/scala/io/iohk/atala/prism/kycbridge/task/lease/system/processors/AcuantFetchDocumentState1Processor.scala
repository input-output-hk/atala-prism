package io.iohk.atala.prism.kycbridge.task.lease.system.processors

import cats.data.EitherT
import doobie.implicits._
import doobie.util.transactor.Transactor
import io.circe.syntax.EncoderOps
import io.iohk.atala.prism.kycbridge.db.ConnectionDao
import io.iohk.atala.prism.kycbridge.services.AssureIdService
import io.iohk.atala.prism.kycbridge.task.lease.system.KycBridgeProcessingTaskState
import io.iohk.atala.prism.kycbridge.task.lease.system.data.{
  AcuantCompareImagesState2Data,
  AcuantFetchDocumentState1Data
}
import io.iohk.atala.prism.services.ConnectorClientService
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

class AcuantFetchDocumentState1Processor(
    tx: Transactor[Task],
    connectorService: ConnectorClientService,
    assureIdService: AssureIdService
) extends ProcessingTaskProcessor[KycBridgeProcessingTaskState] {

  private implicit val logger = LoggerFactory.getLogger(this.getClass)

  override def process(
      processingTask: ProcessingTask[KycBridgeProcessingTaskState]
  ): Task[ProcessingTaskResult[KycBridgeProcessingTaskState]] = {
    (for {
      acuantData <- parseProcessingTaskData[AcuantFetchDocumentState1Data, KycBridgeProcessingTaskState](processingTask)

      connection <- EitherT(
        ConnectionUtils
          .fromConnectionId(acuantData.connectionId, acuantData.receivedMessageId, ConnectionDao.findByConnectionId)
          .logSQLErrors("getting connection from received message", logger)
          .transact(tx)
          .sendResponseOnError(connectorService, acuantData.receivedMessageId, acuantData.connectionId)
          .mapErrorToProcessingTaskFinished[KycBridgeProcessingTaskState]()
      )

      documentStatus <- EitherT(
        assureIdService
          .getDocumentStatus(acuantData.documentInstanceId)
          .logErrorIfPresent
          .mapErrorToProcessingTaskScheduled(processingTask)
      )

      document <- EitherT(
        assureIdService
          .getDocument(acuantData.documentInstanceId)
          .logErrorIfPresent
          .mapErrorToProcessingTaskScheduled(processingTask)
      )

      _ <- EitherT.right[ProcessingTaskResult[KycBridgeProcessingTaskState]](
        ConnectionDao
          .update(connection.copy(acuantDocumentStatus = Some(documentStatus)))
          .logSQLErrors("updating connection", logger)
          .transact(tx)
      )

      state2Data = AcuantCompareImagesState2Data.fromAcuantFetchDocumentState1Data(acuantData, document)
    } yield ProcessingTaskResult.ProcessingTaskStateTransition(
      KycBridgeProcessingTaskState.AcuantCompareImagesState2,
      ProcessingTaskData(state2Data.asJson)
    )).value
      .map(_.merge)
  }
}
