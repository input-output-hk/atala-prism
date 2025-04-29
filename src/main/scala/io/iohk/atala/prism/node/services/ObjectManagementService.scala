package io.iohk.atala.prism.node.services

import cats.Applicative
import cats.ApplicativeError
import cats.Comonad
import cats.Functor
import cats.Monad
import cats.data.EitherT
import cats.effect.MonadCancelThrow
import cats.effect.Resource
import cats.syntax.comonad._
import cats.syntax.either._
import cats.syntax.traverse._
import derevo.derive
import derevo.tagless.applyK
import doobie.free.connection.ConnectionIO
import doobie.implicits._
import doobie.util.transactor.Transactor
import io.iohk.atala.prism.node.cardano.LAST_SYNCED_BLOCK_TIMESTAMP
import io.iohk.atala.prism.node.errors.NodeError
import io.iohk.atala.prism.node.errors.NodeError.InvalidArgument
import io.iohk.atala.prism.node.errors.NodeError.UnsupportedProtocolVersion
import io.iohk.atala.prism.node.metrics.OperationsCounters
import io.iohk.atala.prism.node.models.AtalaObjectTransactionSubmissionStatus.InLedger
import io.iohk.atala.prism.node.models.AtalaOperationId
import io.iohk.atala.prism.node.models.TransactionId
import io.iohk.atala.prism.node.models.TransactionInfo
import io.iohk.atala.prism.node.models._
import io.iohk.atala.prism.node.models.nodeState.getLastSyncedTimestampFromMaybe
import io.iohk.atala.prism.node.operations.CreateDIDOperation
import io.iohk.atala.prism.node.operations.parseOperationWithMockedLedger
import io.iohk.atala.prism.node.operations.protocolVersion.SUPPORTED_VERSION
import io.iohk.atala.prism.node.repositories.AtalaObjectsTransactionsRepository
import io.iohk.atala.prism.node.repositories.AtalaOperationsRepository
import io.iohk.atala.prism.node.repositories.KeyValuesRepository
import io.iohk.atala.prism.node.repositories.ProtocolVersionRepository
import io.iohk.atala.prism.node.repositories.daos.AtalaObjectTransactionSubmissionsDAO
import io.iohk.atala.prism.node.repositories.daos.AtalaObjectsDAO
import io.iohk.atala.prism.node.services.ObjectManagementService.SaveObjectError
import io.iohk.atala.prism.node.services.logs.ObjectManagementServiceLogs
import io.iohk.atala.prism.node.services.models.AtalaObjectNotification
import io.iohk.atala.prism.node.utils.syntax.DBConnectionOps
import io.iohk.atala.prism.protos.node_models
import io.iohk.atala.prism.protos.node_models.SignedAtalaOperation
import tofu.higherKind.Mid
import tofu.logging.Logs
import tofu.logging.ServiceLogging
import tofu.logging.derivation.loggable
import tofu.syntax.feither._
import tofu.syntax.monadic._
import org.slf4j.LoggerFactory
import java.time.Instant

@derive(applyK)
trait ObjectManagementService[F[_]] {
  def saveObject(
      notification: AtalaObjectNotification
  ): F[Either[SaveObjectError, Boolean]]

  def saveObjects(
      notifications: List[AtalaObjectNotification]
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
    didServicesLimit: Int,
    xa: Transactor[F]
) extends ObjectManagementService[F] {
  private val logger = LoggerFactory.getLogger(getClass)

  def saveObjects2(
      notifications: List[AtalaObjectNotification]
  ): F[Either[SaveObjectError, Boolean]] = {
    if (notifications.isEmpty) {
      true.asRight[SaveObjectError].pure[F]
    } else {

      def applyTransactions(atalaObjectInfos: List[AtalaObjectInfo]): F[Either[SaveObjectError, Boolean]] = {
        for {
          // Process all objects and apply them to the state
          transactions <- atalaObjectInfos.traverse { case (obj) =>
            Monad[F].pure(processObject(obj))
          }
          result <- transactions.sequence.flatTraverse { txs =>
            txs
              .traverse(_.logSQLErrorsV2("Transaction saving objects").attemptSql)
              .transact(xa)
              .map(_.sequence)
              .map(_.map(_ => true))
              .leftMapIn(err => SaveObjectError(err.getMessage))
          }

        } yield result
      }

      def processObjectList(
          notifications: List[AtalaObjectNotification]
      ): F[Either[SaveObjectError, Boolean]] = {
        val pairedInserts: List[
          (AtalaObjectId, AtalaObjectsDAO.AtalaObjectCreateData, AtalaObjectsDAO.AtalaObjectSetTransactionInfo)
        ] =
          notifications.map { notification =>
            val objectBytes = notification.atalaObject.toByteArray
            val objId = AtalaObjectId.of(objectBytes)
            (
              objId,
              AtalaObjectsDAO.AtalaObjectCreateData(
                objId,
                objectBytes,
                AtalaObjectStatus.Processed
              ),
              AtalaObjectsDAO.AtalaObjectSetTransactionInfo(
                objId,
                notification.transaction
              )
            )
          }

        val (objectIds, objectInserts, transactionInserts) = pairedInserts.unzip3

        // Bulk database operations
        val bulkQuery = for {
          count1 <- AtalaObjectsDAO.insertMany(objectInserts)
          count2 <- AtalaObjectsDAO.setManyTransactionInfo(transactionInserts)
          count3 <- AtalaObjectTransactionSubmissionsDAO
            .updateStatusBatch(
              transactionInserts.map(d =>
                (
                  d.transactionInfo.ledger,
                  d.transactionInfo.transactionId,
                  AtalaObjectTransactionSubmissionStatus.InLedger
                )
              )
            )
          atalaObjectsInfo <- AtalaObjectsDAO.getAtalaObjectsInfo(objectIds)
        } yield (count1, count2, count3, atalaObjectsInfo)

        bulkQuery
          .logSQLErrorsV2("bulk processing atala objects")
          .attemptSql
          .transact(xa)
          .flatMap {
            case Left(err) => SaveObjectError(err.getMessage).asLeft[Boolean].pure[F]
            case Right((count1, count2, count3, atalaObjectsInfo)) =>
              if (
                count1 != objectInserts.size || count2 != transactionInserts.size || count3 != transactionInserts.size
              ) {
                logger.info(
                  s"Count mismatches: Create(exp=${objectInserts.size},got=$count1), " +
                    s"TxInfo(exp=${transactionInserts.size},got=$count2), " +
                    s"Status(exp=${transactionInserts.size},got=$count3)"
                )
              }
              // Apply transactions to the state finally this sequencial operation similar to applyTransaction
              applyTransactions(atalaObjectsInfo)
          }
      }

      processObjectList(notifications)
    }
  }

  def saveObjects(
      notifications: List[AtalaObjectNotification]
  ): F[Either[SaveObjectError, Boolean]] = {
    if (notifications.isEmpty) {
      true.asRight[SaveObjectError].pure[F]
    } else {

      def applyTransactions(atalaObjectInfos: List[AtalaObjectInfo]): F[Either[SaveObjectError, Boolean]] = {
        processObjects(atalaObjectInfos) match {
          case Left(err) => err.asLeft[Boolean].pure[F]
          case Right(io) =>
            io.attemptSql.transact(xa).map {
              case Left(e) =>
                SaveObjectError(e.getMessage).asLeft[Boolean]
              case Right(result) =>
                result.asRight[SaveObjectError]
            }
        }
      }

      def processObjectList(
          notifications: List[AtalaObjectNotification]
      ): F[Either[SaveObjectError, Boolean]] = {
        val pairedInserts: List[
          (AtalaObjectId, AtalaObjectsDAO.AtalaObjectCreateData, AtalaObjectsDAO.AtalaObjectSetTransactionInfo)
        ] =
          notifications.map { notification =>
            val objectBytes = notification.atalaObject.toByteArray
            val objId = AtalaObjectId.of(objectBytes)
            (
              objId,
              AtalaObjectsDAO.AtalaObjectCreateData(
                objId,
                objectBytes,
                AtalaObjectStatus.Processed
              ),
              AtalaObjectsDAO.AtalaObjectSetTransactionInfo(
                objId,
                notification.transaction
              )
            )
          }

        val (objectIds, objectInserts, transactionInserts) = pairedInserts.unzip3

        // Bulk database operations
        val bulkQuery = for {
          count1 <- AtalaObjectsDAO.insertMany(objectInserts)
          count2 <- AtalaObjectsDAO.setManyTransactionInfo(transactionInserts)
          count3 <- AtalaObjectTransactionSubmissionsDAO
            .updateStatusBatch(
              transactionInserts.map(d =>
                (
                  d.transactionInfo.ledger,
                  d.transactionInfo.transactionId,
                  AtalaObjectTransactionSubmissionStatus.InLedger
                )
              )
            )
          atalaObjectsInfo <- AtalaObjectsDAO.getAtalaObjectsInfo(objectIds)
        } yield (count1, count2, count3, atalaObjectsInfo)

        bulkQuery
          .logSQLErrorsV2("bulk processing atala objects")
          .attemptSql
          .transact(xa)
          .flatMap {
            case Left(err) => SaveObjectError(err.getMessage).asLeft[Boolean].pure[F]
            case Right((count1, count2, count3, atalaObjectsInfo)) =>
              if (
                count1 != objectInserts.size || count2 != transactionInserts.size || count3 != transactionInserts.size
              ) {
                println(
                  s"Count mismatches: Create(exp=${objectInserts.size},got=$count1), " +
                    s"TxInfo(exp=${transactionInserts.size},got=$count2), " +
                    s"Status(exp=${transactionInserts.size},got=$count3)"
                )
              }
              // Apply transactions to the state finally this sequencial operation similar to applyTransaction
              applyTransactions(atalaObjectsInfo)
          }
      }

      processObjectList(notifications)
    }
  }

  // Processes AtalaObjects retrieved from transaction metadata during the Node syncing with Cardano Ledger
  def saveObject(
      notification: AtalaObjectNotification
  ): F[Either[SaveObjectError, Boolean]] = {

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
          // success case
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

  private def processObjects(
      objs: List[AtalaObjectInfo]
  ): Either[SaveObjectError, ConnectionIO[Boolean]] = {
    // Parse all objects, collecting errors and valid BlockInfos
    val parsed: List[Either[SaveObjectError, (BlockProcessingInfo, AtalaObjectInfo)]] = objs.map { obj =>
      for {
        protobufObject <-
          Either
            .fromTry(node_models.AtalaObject.validate(obj.byteContent))
            .leftMap(err => SaveObjectError(err.getMessage))
        result <- protobufObject.blockContent match {
          case Some(block) =>
            for {
              transactionInfo <- Either.fromOption(
                obj.transaction,
                SaveObjectError("AtalaObject has no transaction info")
              )
              transactionBlock <- Either.fromOption(
                transactionInfo.block,
                SaveObjectError("AtalaObject has no transaction block")
              )
            } yield (
              BlockProcessingInfo(
                block,
                transactionInfo.transactionId,
                transactionInfo.ledger,
                transactionBlock.timestamp,
                transactionBlock.index
              ),
              obj
            )
          case None =>
            val msg =
              s"processObjects: blockContent is None for object: ${obj.objectId} transaction: ${obj.transaction}"
            Left(SaveObjectError(msg))
        }
      } yield result
    }

    // Separate errors and valid parses
    val (errors, valid) = parsed.partitionMap(identity)
    val blockInfos = valid.map(_._1)
    val validObjs = valid.map(_._2)

    if (blockInfos.isEmpty)
      Left(errors.headOption.getOrElse(SaveObjectError("No valid objects to process")))
    else
      Right(
        for {
          wasProcessed <- blockProcessing.processBlockBatch(blockInfos) // TODO: Batch processing below
          _ <- AtalaObjectsDAO.updateObjectStatusBatch(
            validObjs.map(_.objectId),
            AtalaObjectStatus.Processed
          )
        } yield wasProcessed
      )
  }
  // Retrieves operations from the object, and applies them to the state
  private def processObject(
      obj: AtalaObjectInfo
  ): Either[SaveObjectError, ConnectionIO[Boolean]] = {
    for {
      // Deserialize object
      protobufObject <-
        Either
          .fromTry(node_models.AtalaObject.validate(obj.byteContent))
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
      .flatMap {
        case CreateDIDOperation(_, keys, services, _, _, _) =>
          if (keys.size > didPublicKeysLimit)
            Left(NodeError.TooManyDidPublicKeysCreationAttempt(didPublicKeysLimit, keys.size))
          else if (services.size > didServicesLimit)
            Left(NodeError.TooManyServiceCreationAttempt(didPublicKeysLimit, services.size))
          else Right(())
        case _ => Right(())
      }
  }
}

object ObjectManagementService {
  @derive(loggable)
  final case class SaveObjectError(msg: String)

  def createAtalaObject(
      ops: List[SignedAtalaOperation]
  ): node_models.AtalaObject = {
    val block = node_models.AtalaBlock(ops)
    node_models
      .AtalaObject()
      .withBlockContent(block)
  }

  def make[I[_]: Functor, F[_]: MonadCancelThrow](
      atalaOperationsRepository: AtalaOperationsRepository[F],
      atalaObjectsTransactionsRepository: AtalaObjectsTransactionsRepository[F],
      keyValuesRepository: KeyValuesRepository[F],
      protocolVersionsRepository: ProtocolVersionRepository[F],
      blockProcessing: BlockProcessingService,
      didPublicKeysLimit: Int,
      didServicesLimit: Int,
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
        didServicesLimit,
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
      didServicesLimit: Int,
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
        didServicesLimit,
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
      didServicesLimit: Int,
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
        didServicesLimit,
        xa,
        logs
      )
      .extract
}
