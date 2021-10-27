package io.iohk.atala.prism.node.services

import cats.data.ReaderT
import cats.data.EitherT
import cats.effect.IO
import cats.implicits._
import doobie.free.connection.ConnectionIO
import doobie.implicits._
import doobie.util.transactor.Transactor
import enumeratum.EnumEntry.Snakecase
import enumeratum.{Enum, EnumEntry}
import io.iohk.atala.prism.connector.AtalaOperationId
import io.iohk.atala.prism.logging.TraceId
import io.iohk.atala.prism.logging.TraceId.IOWithTraceIdContext
import io.iohk.atala.prism.models.TransactionInfo
import io.iohk.atala.prism.node.cardano.LAST_SYNCED_BLOCK_TIMESTAMP
import io.iohk.atala.prism.node.errors.NodeError
import io.iohk.atala.prism.node.errors.NodeError.UnsupportedProtocolVersion
import io.iohk.atala.prism.node.metrics.OperationsCounters
import io.iohk.atala.prism.node.models.AtalaObjectTransactionSubmissionStatus.InLedger
import io.iohk.atala.prism.node.models._
import io.iohk.atala.prism.node.models.nodeState.getLastSyncedTimestampFromMaybe
import io.iohk.atala.prism.node.operations.protocolVersion.SUPPORTED_VERSION
import io.iohk.atala.prism.node.repositories.daos.AtalaObjectsDAO
import io.iohk.atala.prism.node.repositories.{
  AtalaObjectsTransactionsRepository,
  AtalaOperationsRepository,
  KeyValuesRepository,
  ProtocolVersionRepository
}
import io.iohk.atala.prism.node.services.models.AtalaObjectNotification
import io.iohk.atala.prism.protos.node_internal.AtalaBlock
import io.iohk.atala.prism.protos.node_models.SignedAtalaOperation
import io.iohk.atala.prism.protos.{node_internal, node_models}
import io.iohk.atala.prism.utils.syntax.DBConnectionOps
import monix.execution.Scheduler
import org.slf4j.LoggerFactory

import java.time.Instant
import scala.concurrent.Future

private class DuplicateAtalaBlock extends Exception
private class DuplicateAtalaOperation extends Exception

class ObjectManagementService private (
    atalaOperationsRepository: AtalaOperationsRepository[IOWithTraceIdContext],
    atalaObjectsTransactionsRepository: AtalaObjectsTransactionsRepository[
      IOWithTraceIdContext
    ],
    keyValuesRepository: KeyValuesRepository[IOWithTraceIdContext],
    protocolVersionsRepository: ProtocolVersionRepository[IOWithTraceIdContext],
    blockProcessing: BlockProcessingService
)(implicit xa: Transactor[IO], scheduler: Scheduler) {

  private val logger = LoggerFactory.getLogger(this.getClass)

  def saveObject(notification: AtalaObjectNotification): Future[Unit] = {
    // TODO: just add the object to processing queue, instead of processing here
    val updateAction = atalaObjectsTransactionsRepository
      .setObjectTransactionDetails(notification)
      .flatMap {
        case Some(obj) =>
          for {
            _ <- atalaObjectsTransactionsRepository
              .updateSubmissionStatusIfExists(
                obj.transaction.get.ledger,
                obj.transaction.get.transactionId,
                InLedger
              )
            transaction <- ReaderT.liftF(processObject(obj))
            result <- ReaderT.liftF(
              transaction
                .logSQLErrors("saving object", logger)
                .attemptSql
                .transact(xa)
                .map { resultEither =>
                  resultEither.left.map { err =>
                    logger.warn(s"Could not process object $obj", err)
                  }
                  ()
                }
            )
          } yield result

        case None =>
          logger.warn(s"Could not save object from notification $notification")
          ReaderT.liftF(IO.unit)
      }

    protocolVersionsRepository
      .ifNodeSupportsCurrentProtocol()
      .flatMap {
        case Right(_) => updateAction
        case Left(currentVersion) => {
          logger.warn(
            s"Node supports $SUPPORTED_VERSION but current protocol version is $currentVersion." +
              s" Therefore saving Atala object received from the blockchain can't be performed."
          )
          ReaderT.liftF(IO.unit)
        }
      }
      .run(TraceId.generateYOLO)
      .unsafeToFuture()
  }

  def scheduleSingleAtalaOperation(
      op: node_models.SignedAtalaOperation
  ): Future[Either[NodeError, AtalaOperationId]] =
    scheduleAtalaOperations(op).map(_.head)

  def scheduleAtalaOperations(
      ops: node_models.SignedAtalaOperation*
  ): Future[List[Either[NodeError, AtalaOperationId]]] = {
    val opsWithObjects = ops.toList.map { op =>
      (op, ObjectManagementService.createAtalaObject(List(op)))
    }

    val queryIO = opsWithObjects traverse { case (op, obj) =>
      val objBytes = obj.toByteArray
      atalaOperationsRepository
        .insertOperation(
          AtalaObjectId.of(objBytes),
          objBytes,
          AtalaOperationId.of(op),
          AtalaOperationStatus.RECEIVED
        )
    }

    val resultEitherT: EitherT[IOWithTraceIdContext, NodeError, List[
      Either[NodeError, AtalaOperationId]
    ]] = for {
      _ <- EitherT(
        protocolVersionsRepository
          .ifNodeSupportsCurrentProtocol()
          .map(_.left.map(UnsupportedProtocolVersion))
      )
      operationInsertions <- EitherT.liftF(queryIO)
    } yield {
      opsWithObjects.zip(operationInsertions).map {
        case ((atalaOperation, _), Left(err)) =>
          OperationsCounters
            .failedToStoreToDbAtalaOperations(List(atalaOperation), err)
          err.asLeft[AtalaOperationId]
        case ((atalaOperation, _), Right(cntAdded)) if cntAdded == ((0, 0)) =>
          val err = NodeError
            .DuplicateAtalaOperation(AtalaOperationId.of(atalaOperation))
          OperationsCounters
            .failedToStoreToDbAtalaOperations(List(atalaOperation), err)
          AtalaOperationId.of(atalaOperation).asRight[NodeError]
        case ((atalaOperation, _), Right(_)) =>
          OperationsCounters.countReceivedAtalaOperations(List(atalaOperation))
          AtalaOperationId.of(atalaOperation).asRight[NodeError]
      }
    }

    resultEitherT.value.run(TraceId.generateYOLO).unsafeToFuture().flatMap {
      case Left(e) => Future.failed(e.toStatus.asRuntimeException)
      case Right(r) => Future.successful(r)
    }
  }

  def getLastSyncedTimestamp: Future[Instant] = {
    for {
      maybeLastSyncedBlockTimestamp <-
        keyValuesRepository
          .get(LAST_SYNCED_BLOCK_TIMESTAMP)
          .run(TraceId.generateYOLO)
          .unsafeToFuture()
      lastSyncedBlockTimestamp = getLastSyncedTimestampFromMaybe(
        maybeLastSyncedBlockTimestamp.value
      )
    } yield lastSyncedBlockTimestamp
  }

  def getOperationInfo(
      atalaOperationId: AtalaOperationId
  ): Future[Option[AtalaOperationInfo]] =
    atalaOperationsRepository
      .getOperationInfo(atalaOperationId)
      .run(TraceId.generateYOLO)
      .unsafeToFuture()

  private def processObject(obj: AtalaObjectInfo): IO[ConnectionIO[Boolean]] = {
    for {
      protobufObject <- IO.fromTry(
        node_internal.AtalaObject.validate(obj.byteContent)
      )
      block = protobufObject.blockContent.get
      transactionInfo = obj.transaction.getOrElse(
        throw new RuntimeException("AtalaObject has no transaction info")
      )
      transactionBlock =
        transactionInfo.block.getOrElse(
          throw new RuntimeException("AtalaObject has no transaction block")
        )
      _ = logBlockRequest("processObject", block, obj)
      blockProcessed = blockProcessing.processBlock(
        block,
        transactionInfo.transactionId,
        transactionInfo.ledger,
        transactionBlock.timestamp,
        transactionBlock.index
      )
    } yield for {
      wasProcessed <- blockProcessed
      _ <- AtalaObjectsDAO.updateObjectStatus(
        obj.objectId,
        AtalaObjectStatus.Processed
      )
    } yield wasProcessed
  }

  private def logBlockRequest(
      methodName: String,
      block: AtalaBlock,
      atalaObject: AtalaObjectInfo
  ): Unit = {
    val operationIds = block.operations.map(AtalaOperationId.of).mkString("\n")
    logger.info(
      s"MethodName:$methodName \n Block OperationIds = [$operationIds \n] atalaObject = $atalaObject"
    )
  }
}

object ObjectManagementService {
  sealed trait AtalaObjectTransactionStatus extends EnumEntry with Snakecase
  object AtalaObjectTransactionStatus extends Enum[AtalaObjectTransactionStatus] {
    val values: IndexedSeq[AtalaObjectTransactionStatus] = findValues

    case object Pending extends AtalaObjectTransactionStatus
    case object InLedger extends AtalaObjectTransactionStatus
    case object Confirmed extends AtalaObjectTransactionStatus
  }

  case class AtalaObjectTransactionInfo(
      transaction: TransactionInfo,
      status: AtalaObjectTransactionStatus
  )

  def createAtalaObject(
      ops: List[SignedAtalaOperation]
  ): node_internal.AtalaObject = {
    val block = node_internal.AtalaBlock(ops)
    node_internal
      .AtalaObject(blockOperationCount = block.operations.size)
      .withBlockContent(block)
  }

  def apply(
      atalaOperationsRepository: AtalaOperationsRepository[
        IOWithTraceIdContext
      ],
      atalaObjectsTransactionsRepository: AtalaObjectsTransactionsRepository[
        IOWithTraceIdContext
      ],
      keyValuesRepository: KeyValuesRepository[IOWithTraceIdContext],
      protocolVersionsRepository: ProtocolVersionRepository[
        IOWithTraceIdContext
      ],
      blockProcessing: BlockProcessingService
  )(implicit
      xa: Transactor[IO],
      scheduler: Scheduler
  ): ObjectManagementService = {
    new ObjectManagementService(
      atalaOperationsRepository,
      atalaObjectsTransactionsRepository,
      keyValuesRepository,
      protocolVersionsRepository,
      blockProcessing
    )
  }
}
