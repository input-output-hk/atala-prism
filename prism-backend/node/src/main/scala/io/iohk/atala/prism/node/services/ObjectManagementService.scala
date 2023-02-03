package io.iohk.atala.prism.node.services

import cats.data.EitherT
import cats.effect.{MonadCancelThrow, Resource}
import cats.syntax.comonad._
import cats.syntax.either._
import cats.syntax.traverse._
import cats.{Applicative, ApplicativeError, Comonad, Functor, Monad}
import derevo.derive
import derevo.tagless.applyK
import doobie.free.connection.ConnectionIO
import doobie.implicits._
import doobie.util.transactor.Transactor
import enumeratum.EnumEntry.Snakecase
import enumeratum.{Enum, EnumEntry}
import io.iohk.atala.prism.models.{AtalaOperationId, TransactionId, TransactionInfo}
import io.iohk.atala.prism.node.cardano.LAST_SYNCED_BLOCK_TIMESTAMP
import io.iohk.atala.prism.node.errors.NodeError
import io.iohk.atala.prism.node.errors.NodeError.{InvalidArgument, UnsupportedProtocolVersion}
import io.iohk.atala.prism.node.metrics.OperationsCounters
import io.iohk.atala.prism.node.models.AtalaObjectTransactionSubmissionStatus.InLedger
import io.iohk.atala.prism.node.models._
import io.iohk.atala.prism.node.models.nodeState.getLastSyncedTimestampFromMaybe
import io.iohk.atala.prism.node.operations.protocolVersion.SUPPORTED_VERSION
import io.iohk.atala.prism.node.operations.{CreateDIDOperation, UpdateDIDOperation, parseOperationWithMockedLedger}
import io.iohk.atala.prism.node.repositories.daos.AtalaObjectsDAO
import io.iohk.atala.prism.node.repositories.{
  AtalaObjectsTransactionsRepository,
  AtalaOperationsRepository,
  KeyValuesRepository,
  ProtocolVersionRepository
}
import io.iohk.atala.prism.node.services.ObjectManagementService.SaveObjectError
import io.iohk.atala.prism.node.services.logs.ObjectManagementServiceLogs
import io.iohk.atala.prism.node.services.models.AtalaObjectNotification
import io.iohk.atala.prism.protos.node_models.SignedAtalaOperation
import io.iohk.atala.prism.protos.{node_internal, node_models}
import io.iohk.atala.prism.utils.syntax.DBConnectionOps
import tofu.higherKind.Mid
import tofu.logging.derivation.loggable
import tofu.logging.{Logs, ServiceLogging}
import tofu.syntax.feither._
import tofu.syntax.monadic._

import java.time.Instant

private class DuplicateAtalaBlock extends Exception
private class DuplicateAtalaOperation extends Exception

@derive(applyK)
trait ObjectManagementService[F[_]] {
  def saveObject(
      notification: AtalaObjectNotification
  ): F[Either[SaveObjectError, Boolean]]

  def scheduleAtalaOperations(
      ops: node_models.SignedAtalaOperation*
  ): F[List[Either[NodeError, AtalaOperationId]]]

  def getScheduledAtalaObjects: F[Either[NodeError, List[AtalaObjectInfo]]]

  def getUnconfirmedTransactions(
      lastSeenTxId: Option[TransactionId],
      limit: Int
  ): F[Either[NodeError, List[TransactionInfo]]]

  def getConfirmedTransactions(
      lastSeenTxId: Option[TransactionId],
      limit: Int
  ): F[Either[NodeError, List[TransactionInfo]]]

  def getLastSyncedTimestamp: F[Instant]

  def getCurrentProtocolVersion: F[ProtocolVersion]

  def getOperationInfo(
      atalaOperationId: AtalaOperationId
  ): F[Option[AtalaOperationInfo]]
}

private final class ObjectManagementServiceImpl[F[_]: MonadCancelThrow](
    atalaOperationsRepository: AtalaOperationsRepository[F],
    atalaObjectsTransactionsRepository: AtalaObjectsTransactionsRepository[F],
    keyValuesRepository: KeyValuesRepository[F],
    protocolVersionsRepository: ProtocolVersionRepository[F],
    blockProcessing: BlockProcessingService,
    didPublicKeysLimit: Int,
    xa: Transactor[F]
) extends ObjectManagementService[F] {

  // Processes AtalaObjects retrieved from transaction metadata during the Node syncing with Cardano Ledger
  def saveObject(
      notification: AtalaObjectNotification
  ): F[Either[SaveObjectError, Boolean]] = {

    def validateObj(obj: AtalaObjectInfo, tx: TransactionInfo): Either[SaveObjectError, Unit] = {
      val errMetadata =
        s"objId: ${obj.objectId.toString}, ledger: ${tx.ledger} txId: ${tx.transactionId}".stripMargin
      node_internal.AtalaObject
        .validate(obj.byteContent)
        .toEither
        .leftMap(err => SaveObjectError(s"""
             | Could not parse AtalaObject protobuff, got error: ${err.getMessage}
             | $errMetadata
             | """.stripMargin))
        .flatMap { parsedObj =>
          val mbBlock = parsedObj.blockContent
          val expectedOpCount = parsedObj.blockOperationCount
          val expectedBlockByteLength = parsedObj.blockByteLength
          mbBlock.fold[Either[SaveObjectError, Unit]](Left(SaveObjectError(s"""
               | AtalaObject does not have AtalaBlock.
               | $errMetadata
               |""".stripMargin))) { block =>
            if (block.operations.size != expectedOpCount) Left(SaveObjectError(s"""
                 | Expected operations count - $expectedOpCount, got - ${block.operations.size}
                 | $errMetadata
                 |""".stripMargin))
            else if (block.toByteArray.length != expectedBlockByteLength) Left(SaveObjectError(s"""
                 | Expected block byte length - $expectedBlockByteLength, got - ${block.toByteArray.length}
                 | $errMetadata
                 |""".stripMargin))
            else Right(()).withLeft[SaveObjectError]
          }
        }
    }

    def applyTransaction(objMaybe: Option[AtalaObjectInfo]): F[Either[SaveObjectError, Boolean]] = {
      objMaybe match {
        case Some(obj) =>
          for {
            // Update transaction submission status to InLedger
            _ <- atalaObjectsTransactionsRepository
              .updateSubmissionStatusIfExists(
                obj.transaction.get.ledger,
                obj.transaction.get.transactionId,
                InLedger
              )

            // Validate AtalaObject and if invalid, do not process
            _ <- Monad[F].pure(validateObj(obj, notification.transaction))
            // Retrieve all operations from the object and apply them to the state.
            // After this method every operation should have either APPROVED_AND_APPLIED or APPROVED_AND_REJECTED status.
            transaction <- Monad[F].pure(processObject(obj))
            // Save the error if processObject failed
            result <- transaction flatTraverse {
              _.logSQLErrorsV2("saving object").attemptSql
                .transact(xa)
                .leftMapIn(err => SaveObjectError(err.getMessage))
            }
          } yield result

        case None =>
          Monad[F].pure(
            SaveObjectError(
              s"Could not save object from notification: txId: ${notification.transaction.transactionId}, ledger: ${notification.transaction.ledger}"
            ).asLeft[Boolean]
          )
      }
    }

    val updateAction = for {
      objectInfoMaybe <- atalaObjectsTransactionsRepository.setObjectTransactionDetails(notification)
      appliedE <- applyTransaction(objectInfoMaybe)
    } yield appliedE

    // Apply operations from the AtalaObject only if Node supports the protocol version corresponding to the public ledger
    protocolVersionsRepository
      .ifNodeSupportsCurrentProtocol()
      .flatMap {
        case Right(_) => updateAction
        case Left(currentVersion) => {
          Monad[F].pure(
            SaveObjectError(
              s"Node supports $SUPPORTED_VERSION but current protocol version is $currentVersion." +
                s" Therefore saving Atala object received from the blockchain can't be performed."
            ).asLeft
          )
        }
      }
  }

  // User calls this rpc method to send new operations. All operations are initially stored with the status RECEIVED.
  def scheduleAtalaOperations(
      ops: node_models.SignedAtalaOperation*
  ): F[List[Either[NodeError, AtalaOperationId]]] = {
    // creates one AtalaObject per operation
    val opsWithObjects = ops.toList.map { op =>
      (op, ObjectManagementService.createAtalaObject(List(op)))
    }

    val queryIO: F[List[Either[NodeError, (Int, Int)]]] =
      opsWithObjects traverse { case (op, obj) =>
        preliminaryCheckOfScheduledOperation(op) match {
          case Right(_) =>
            val objBytes = obj.toByteArray
            atalaOperationsRepository
              // inserts AtalaObject (with status - Scheduled) and AtalaOperation (with status - Received)
              // AtalaObject has AtalaBlock with only one AtalaOperation inside of it.
              // AtalaOperation will have object_id in db, which is associated with an object
              // that has that operation
              .insertOperation(
                AtalaObjectId.of(objBytes),
                objBytes,
                AtalaOperationId.of(op),
                AtalaOperationStatus.RECEIVED
              )
          case Left(e) => e.asLeft[(Int, Int)].pure[F]
        }
      }

    val resultEitherT: EitherT[F, NodeError, List[Either[NodeError, AtalaOperationId]]] =
      for {
        // don't schedule new operations if the version of the Node is outdated
        _ <- EitherT(
          protocolVersionsRepository
            .ifNodeSupportsCurrentProtocol()
            .map(_.left.map(UnsupportedProtocolVersion))
        )
        // save operations to the database with status RECEIVED
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
            OperationsCounters
              .countReceivedAtalaOperations(List(atalaOperation))
            AtalaOperationId.of(atalaOperation).asRight[NodeError]
        }
      }

    resultEitherT.value.flatMap {
      case Left(e) =>
        ApplicativeError[F, Throwable].raiseError(e.toStatus.asRuntimeException)
      case Right(r) => Applicative[F].pure(r)
    }
  }

  override def getScheduledAtalaObjects: F[Either[NodeError, List[AtalaObjectInfo]]] =
    atalaObjectsTransactionsRepository.getNotProcessedObjects

  override def getUnconfirmedTransactions(
      lastSeenTxId: Option[TransactionId],
      limit: Int
  ): F[Either[NodeError, List[TransactionInfo]]] = {
    val query = for {
      _ <- EitherT.cond[F](limit <= 50, (), InvalidArgument(s"limit value $limit > 50"))
      result <- EitherT(atalaObjectsTransactionsRepository.getUnconfirmedObjectTransactions(lastSeenTxId, limit))
    } yield result

    query.value
  }

  override def getConfirmedTransactions(
      lastSeenTxId: Option[TransactionId],
      limit: Int
  ): F[Either[NodeError, List[TransactionInfo]]] = {
    val query = for {
      _ <- EitherT.cond[F](limit <= 50, (), InvalidArgument(s"limit value $limit > 50"))
      result <- EitherT(atalaObjectsTransactionsRepository.getConfirmedObjectTransactions(lastSeenTxId, limit))
    } yield result

    query.value
  }

  def getLastSyncedTimestamp: F[Instant] =
    for {
      maybeLastSyncedBlockTimestamp <-
        keyValuesRepository
          .get(LAST_SYNCED_BLOCK_TIMESTAMP)
      lastSyncedBlockTimestamp = getLastSyncedTimestampFromMaybe(
        maybeLastSyncedBlockTimestamp.value
      )
    } yield lastSyncedBlockTimestamp

  def getCurrentProtocolVersion: F[ProtocolVersion] =
    protocolVersionsRepository.getCurrentProtocolVersion()

  def getOperationInfo(
      atalaOperationId: AtalaOperationId
  ): F[Option[AtalaOperationInfo]] =
    atalaOperationsRepository
      .getOperationInfo(atalaOperationId)
      .map(_.toOption.flatten)

  // Retrieves operations from the object, and applies them to the state
  private def processObject(
      obj: AtalaObjectInfo
  ): Either[SaveObjectError, ConnectionIO[Boolean]] = {
    for {
      // Deserialize object
      protobufObject <-
        Either
          .fromTry(node_internal.AtalaObject.validate(obj.byteContent))
          .leftMap(err => SaveObjectError(err.getMessage))
      block = protobufObject.blockContent.get
      // Retrieve transaction info (transaction identifier, name of the ledger)
      transactionInfo <- Either.fromOption(
        obj.transaction,
        SaveObjectError("AtalaObject has no transaction info")
      )
      // Retrieve block of operations
      transactionBlock <-
        Either.fromOption(
          transactionInfo.block,
          SaveObjectError("AtalaObject has no transaction block")
        )
      // Apply operations from the block, update statuses according to the ledger information
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

  private def preliminaryCheckOfScheduledOperation(signedOperation: SignedAtalaOperation): Either[NodeError, Unit] = {
    parseOperationWithMockedLedger(signedOperation).left
      .map { validationError =>
        NodeError.UnableToParseSignedOperation(validationError.explanation): NodeError
      }
      .flatMap(op =>
        op match {
          case CreateDIDOperation(_, keys, _, _, _) if keys.size > didPublicKeysLimit =>
            Left(NodeError.TooManyDidPublicKeysAccessAttempt(didPublicKeysLimit, Some(keys.size)))
          case UpdateDIDOperation(_, actions, _, _, _) if actions.size > didPublicKeysLimit =>
            Left(NodeError.TooManyDidPublicKeysAccessAttempt(didPublicKeysLimit, Some(actions.size)))
          case _ => Right(())
        }
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

  @derive(loggable)
  final case class SaveObjectError(msg: String)

  def createAtalaObject(
      ops: List[SignedAtalaOperation]
  ): node_internal.AtalaObject = {
    val block = node_internal.AtalaBlock(ops)
    node_internal
      .AtalaObject()
      .withBlockContent(block)
      .withBlockOperationCount(block.operations.size)
      .withBlockByteLength(block.toByteArray.length)
  }

  def make[I[_]: Functor, F[_]: MonadCancelThrow](
      atalaOperationsRepository: AtalaOperationsRepository[F],
      atalaObjectsTransactionsRepository: AtalaObjectsTransactionsRepository[F],
      keyValuesRepository: KeyValuesRepository[F],
      protocolVersionsRepository: ProtocolVersionRepository[F],
      blockProcessing: BlockProcessingService,
      didPublicKeysLimit: Int,
      xa: Transactor[F],
      logs: Logs[I, F]
  ): I[ObjectManagementService[F]] = {
    for {
      serviceLogs <- logs.service[ObjectManagementService[F]]
    } yield {
      implicit val implicitLogs: ServiceLogging[F, ObjectManagementService[F]] =
        serviceLogs
      val logs: ObjectManagementService[Mid[F, *]] =
        new ObjectManagementServiceLogs[F]
      val mid = logs
      mid attach new ObjectManagementServiceImpl[F](
        atalaOperationsRepository,
        atalaObjectsTransactionsRepository,
        keyValuesRepository,
        protocolVersionsRepository,
        blockProcessing,
        didPublicKeysLimit,
        xa
      )
    }
  }

  def resource[I[_]: Applicative: Functor, F[_]: MonadCancelThrow](
      atalaOperationsRepository: AtalaOperationsRepository[F],
      atalaObjectsTransactionsRepository: AtalaObjectsTransactionsRepository[F],
      keyValuesRepository: KeyValuesRepository[F],
      protocolVersionsRepository: ProtocolVersionRepository[F],
      blockProcessing: BlockProcessingService,
      didPublicKeysLimit: Int,
      xa: Transactor[F],
      logs: Logs[I, F]
  ): Resource[I, ObjectManagementService[F]] = Resource.eval(
    ObjectManagementService
      .make(
        atalaOperationsRepository,
        atalaObjectsTransactionsRepository,
        keyValuesRepository,
        protocolVersionsRepository,
        blockProcessing,
        didPublicKeysLimit,
        xa,
        logs
      )
  )

  def unsafe[I[_]: Comonad, F[_]: MonadCancelThrow](
      atalaOperationsRepository: AtalaOperationsRepository[F],
      atalaObjectsTransactionsRepository: AtalaObjectsTransactionsRepository[F],
      keyValuesRepository: KeyValuesRepository[F],
      protocolVersionsRepository: ProtocolVersionRepository[F],
      blockProcessing: BlockProcessingService,
      didPublicKeysLimit: Int,
      xa: Transactor[F],
      logs: Logs[I, F]
  ): ObjectManagementService[F] =
    ObjectManagementService
      .make(
        atalaOperationsRepository,
        atalaObjectsTransactionsRepository,
        keyValuesRepository,
        protocolVersionsRepository,
        blockProcessing,
        didPublicKeysLimit,
        xa,
        logs
      )
      .extract
}
