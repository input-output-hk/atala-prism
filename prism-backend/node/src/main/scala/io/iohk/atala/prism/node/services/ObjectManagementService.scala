package io.iohk.atala.prism.node.services

import cats.data.EitherT
import cats.effect.Sync
import cats.syntax.comonad._
import cats.syntax.either._
import cats.syntax.traverse._
import tofu.syntax.monadic._
import cats.{Comonad, Functor}
import derevo.derive
import derevo.tagless.applyK
import doobie.free.connection.ConnectionIO
import doobie.implicits._
import doobie.util.transactor.Transactor
import enumeratum.EnumEntry.Snakecase
import enumeratum.{Enum, EnumEntry}
import io.iohk.atala.prism.connector.AtalaOperationId
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
import io.iohk.atala.prism.node.services.logs.ObjectManagementServiceLogs
import io.iohk.atala.prism.node.services.models.AtalaObjectNotification
import io.iohk.atala.prism.protos.node_internal.AtalaBlock
import io.iohk.atala.prism.protos.node_models.SignedAtalaOperation
import io.iohk.atala.prism.protos.{node_internal, node_models}
import io.iohk.atala.prism.utils.syntax.DBConnectionOps
import org.slf4j.LoggerFactory
import tofu.higherKind.Mid
import tofu.logging.{Logs, ServiceLogging}

import java.time.Instant

private class DuplicateAtalaBlock extends Exception
private class DuplicateAtalaOperation extends Exception

@derive(applyK)
trait ObjectManagementService[F[_]] {
  def saveObject(notification: AtalaObjectNotification): F[Unit]

  def scheduleSingleAtalaOperation(op: node_models.SignedAtalaOperation): F[Either[NodeError, AtalaOperationId]]

  def scheduleAtalaOperations(ops: node_models.SignedAtalaOperation*): F[List[Either[NodeError, AtalaOperationId]]]

  def getLastSyncedTimestamp: F[Instant]

  def getOperationInfo(atalaOperationId: AtalaOperationId): F[Option[AtalaOperationInfo]]
}

private final class ObjectManagementServiceImpl[F[_]: Sync](
    atalaOperationsRepository: AtalaOperationsRepository[F],
    atalaObjectsTransactionsRepository: AtalaObjectsTransactionsRepository[F],
    keyValuesRepository: KeyValuesRepository[F],
    protocolVersionsRepository: ProtocolVersionRepository[F],
    blockProcessing: BlockProcessingService
)(implicit xa: Transactor[F])
    extends ObjectManagementService[F] {

  private val logger = LoggerFactory.getLogger(this.getClass)

  def saveObject(notification: AtalaObjectNotification): F[Unit] = {
    // TODO: just add the object to processing queue, instead of processing here
    val updateAction = atalaObjectsTransactionsRepository
      .setObjectTransactionDetails(notification)
      .flatMap {
        case Some(obj) =>
          for {
            _ <- atalaObjectsTransactionsRepository.updateSubmissionStatusIfExists(
              obj.transaction.get.ledger,
              obj.transaction.get.transactionId,
              InLedger
            )
            transaction <- processObject(obj)
            _ <-
              transaction
                .logSQLErrors("saving object", logger)
                .attemptSql
                .transact(xa)
          } yield ()

        case None =>
          Sync[F].delay(logger.warn(s"Could not save object from notification $notification"))

      }

    protocolVersionsRepository
      .ifNodeSupportsCurrentProtocol()
      .flatMap {
        case Right(_) => updateAction
        case Left(currentVersion) => {
          Sync[F].delay(
            logger.warn(
              s"Node supports $SUPPORTED_VERSION but current protocol version is $currentVersion." +
                s" Therefore saving Atala object received from the blockchain can't be performed."
            )
          )
        }
      }
  }

  def scheduleSingleAtalaOperation(op: node_models.SignedAtalaOperation): F[Either[NodeError, AtalaOperationId]] =
    scheduleAtalaOperations(op).map(_.head)

  def scheduleAtalaOperations(
      ops: node_models.SignedAtalaOperation*
  ): F[List[Either[NodeError, AtalaOperationId]]] = {
    val opsWithObjects = ops.toList.map { op =>
      (op, ObjectManagementService.createAtalaObject(List(op)))
    }

    val queryIO: F[List[Either[NodeError, (Int, Int)]]] = opsWithObjects traverse {
      case (op, obj) =>
        val objBytes = obj.toByteArray
        atalaOperationsRepository
          .insertOperation(
            AtalaObjectId.of(objBytes),
            objBytes,
            AtalaOperationId.of(op),
            AtalaOperationStatus.RECEIVED
          )
    }

    val resultEitherT: EitherT[F, NodeError, List[Either[NodeError, AtalaOperationId]]] = for {
      _ <- EitherT(
        protocolVersionsRepository
          .ifNodeSupportsCurrentProtocol()
          .map(_.left.map(UnsupportedProtocolVersion))
      )
      operationInsertions <- EitherT.liftF(queryIO)
    } yield {
      opsWithObjects.zip(operationInsertions).map {
        case ((atalaOperation, _), Left(err)) =>
          OperationsCounters.failedToStoreToDbAtalaOperations(List(atalaOperation), err)
          err.asLeft[AtalaOperationId]
        case ((atalaOperation, _), Right(cntAdded)) if cntAdded == ((0, 0)) =>
          val err = NodeError.DuplicateAtalaOperation(AtalaOperationId.of(atalaOperation))
          OperationsCounters.failedToStoreToDbAtalaOperations(List(atalaOperation), err)
          AtalaOperationId.of(atalaOperation).asRight[NodeError]
        case ((atalaOperation, _), Right(_)) =>
          OperationsCounters.countReceivedAtalaOperations(List(atalaOperation))
          AtalaOperationId.of(atalaOperation).asRight[NodeError]
      }
    }

    resultEitherT.value.flatMap {
      case Left(e) => Sync[F].raiseError(e.toStatus.asRuntimeException)
      case Right(r) => Sync[F].pure(r)
    }
  }

  def getLastSyncedTimestamp: F[Instant] =
    for {
      maybeLastSyncedBlockTimestamp <-
        keyValuesRepository
          .get(LAST_SYNCED_BLOCK_TIMESTAMP)
      lastSyncedBlockTimestamp = getLastSyncedTimestampFromMaybe(maybeLastSyncedBlockTimestamp.value)
    } yield lastSyncedBlockTimestamp

  def getOperationInfo(atalaOperationId: AtalaOperationId): F[Option[AtalaOperationInfo]] =
    atalaOperationsRepository
      .getOperationInfo(atalaOperationId)

  private def processObject(obj: AtalaObjectInfo): F[ConnectionIO[Boolean]] = {
    for {
      protobufObject <- Sync[F].fromTry(node_internal.AtalaObject.validate(obj.byteContent))
      block = protobufObject.blockContent.get
      transactionInfo = obj.transaction.getOrElse(throw new RuntimeException("AtalaObject has no transaction info"))
      transactionBlock =
        transactionInfo.block.getOrElse(throw new RuntimeException("AtalaObject has no transaction block"))
      _ <- logBlockRequest("processObject", block, obj)
      blockProcessed = blockProcessing.processBlock(
        block,
        transactionInfo.transactionId,
        transactionInfo.ledger,
        transactionBlock.timestamp,
        transactionBlock.index
      )
    } yield for {
      wasProcessed <- blockProcessed
      _ <- AtalaObjectsDAO.updateObjectStatus(obj.objectId, AtalaObjectStatus.Processed)
    } yield wasProcessed
  }

  private def logBlockRequest(methodName: String, block: AtalaBlock, atalaObject: AtalaObjectInfo): F[Unit] = {
    Sync[F].delay {
      val operationIds = block.operations.map(AtalaOperationId.of).mkString("\n")
      logger.info(
        s"MethodName:$methodName \n Block OperationIds = [$operationIds \n] atalaObject = $atalaObject"
      )
    }
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

  case class AtalaObjectTransactionInfo(transaction: TransactionInfo, status: AtalaObjectTransactionStatus)

  def createAtalaObject(ops: List[SignedAtalaOperation]): node_internal.AtalaObject = {
    val block = node_internal.AtalaBlock(ATALA_OBJECT_VERSION, ops)
    node_internal.AtalaObject(blockOperationCount = block.operations.size).withBlockContent(block)
  }

  def make[I[_]: Functor, F[_]: Sync](
      atalaOperationsRepository: AtalaOperationsRepository[F],
      atalaObjectsTransactionsRepository: AtalaObjectsTransactionsRepository[F],
      keyValuesRepository: KeyValuesRepository[F],
      protocolVersionsRepository: ProtocolVersionRepository[F],
      blockProcessing: BlockProcessingService
  )(implicit xa: Transactor[F], logs: Logs[I, F]): I[ObjectManagementService[F]] = {
    for {
      serviceLogs <- logs.service[ObjectManagementService[F]]
    } yield {
      implicit val implicitLogs: ServiceLogging[F, ObjectManagementService[F]] = serviceLogs
      val logs: ObjectManagementService[Mid[F, *]] = new ObjectManagementServiceLogs[F]
      val mid = logs
      mid attach new ObjectManagementServiceImpl[F](
        atalaOperationsRepository,
        atalaObjectsTransactionsRepository,
        keyValuesRepository,
        protocolVersionsRepository,
        blockProcessing
      )
    }
  }

  def unsafe[I[_]: Comonad, F[_]: Sync](
      atalaOperationsRepository: AtalaOperationsRepository[F],
      atalaObjectsTransactionsRepository: AtalaObjectsTransactionsRepository[F],
      keyValuesRepository: KeyValuesRepository[F],
      protocolVersionsRepository: ProtocolVersionRepository[F],
      blockProcessing: BlockProcessingService
  )(implicit xa: Transactor[F], logs: Logs[I, F]): ObjectManagementService[F] =
    ObjectManagementService
      .make(
        atalaOperationsRepository,
        atalaObjectsTransactionsRepository,
        keyValuesRepository,
        protocolVersionsRepository,
        blockProcessing
      )(
        Functor[I],
        Sync[F],
        xa,
        logs
      )
      .extract
}
