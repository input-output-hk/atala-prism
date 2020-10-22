package io.iohk.atala.prism.node.services

import java.time.{Duration, Instant}

import cats.effect.IO
import com.google.protobuf.ByteString
import doobie.free.connection
import doobie.free.connection.ConnectionIO
import doobie.implicits._
import doobie.util.transactor.Transactor
import io.iohk.atala.prism.crypto.SHA256Digest
import io.iohk.atala.prism.models.{TransactionInfo, TransactionStatus}
import io.iohk.atala.prism.node.AtalaReferenceLedger
import io.iohk.atala.prism.node.models.{
  AtalaObject,
  AtalaObjectTransactionSubmission,
  AtalaObjectTransactionSubmissionStatus
}
import io.iohk.atala.prism.node.objects.ObjectStorageService
import io.iohk.atala.prism.node.repositories.daos.AtalaObjectsDAO.{AtalaObjectCreateData, AtalaObjectSetTransactionInfo}
import io.iohk.atala.prism.node.repositories.daos.{AtalaObjectTransactionSubmissionsDAO, AtalaObjectsDAO}
import io.iohk.atala.prism.node.services.ObjectManagementService.Config
import io.iohk.atala.prism.node.services.models.AtalaObjectNotification
import io.iohk.atala.prism.protos.node_internal.AtalaObject.Block
import io.iohk.atala.prism.protos.{node_internal, node_models}
import monix.execution.Scheduler
import org.slf4j.LoggerFactory

import scala.concurrent.Future
import scala.concurrent.duration._

private class AtalaObjectCannotBeModified extends Exception
private class AtalaObjectAlreadyPublished extends Exception

class ObjectManagementService private (
    config: Config,
    storage: ObjectStorageService,
    atalaReferenceLedger: AtalaReferenceLedger,
    blockProcessing: BlockProcessingService
)(implicit xa: Transactor[IO], scheduler: Scheduler) {

  private val logger = LoggerFactory.getLogger(this.getClass)

  // Schedule first run
  scheduleRetryOldPendingTransactions(config.ledgerPendingTransactionSyncDelay)

  private def setObjectTransactionDetails(notification: AtalaObjectNotification): Future[Option[AtalaObject]] = {
    val objectBytes = notification.atalaObject.toByteArray
    val hash = SHA256Digest.compute(objectBytes)

    val query = for {
      existingObject <- AtalaObjectsDAO.get(hash)
      _ <- {
        existingObject match {
          // Object previously found in the blockchain
          case Some(obj) if obj.transaction.isDefined => connection.raiseError(new AtalaObjectCannotBeModified)
          // Object previously saved in DB, but not in the blockchain
          case Some(_) => connection.unit
          // Object was not in DB, save it to populate transaction data below
          case None => AtalaObjectsDAO.insert(AtalaObjectCreateData(hash, objectBytes))
        }
      }

      _ = notification.transaction.block.getOrElse(
        throw new IllegalArgumentException("Transaction has no block")
      )
      _ <- AtalaObjectsDAO.setTransactionInfo(
        AtalaObjectSetTransactionInfo(
          hash,
          notification.transaction
        )
      )
      obj <- AtalaObjectsDAO.get(hash)
    } yield obj

    query
      .transact(xa)
      .unsafeToFuture()
      .recover {
        case _: AtalaObjectCannotBeModified => None
      }
  }

  def saveObject(notification: AtalaObjectNotification): Future[Unit] = {
    // TODO: just add the object to processing queue, instead of processing here
    setObjectTransactionDetails(notification)
      .flatMap {
        case Some(obj) =>
          processObject(obj).flatMap { transaction =>
            transaction.transact(xa).unsafeToFuture().map(_ => ())
          } recover {
            case error => logger.warn(s"Could not process object $obj", error)
          }
        case None =>
          logger.warn(s"Could not save object from notification $notification")
          Future.successful(())
      }
  }

  def publishAtalaOperation(op: node_models.SignedAtalaOperation): Future[TransactionInfo] = {
    val block = node_internal.AtalaBlock("1.0", List(op))
    val blockBytes = block.toByteArray
    val blockHash = SHA256Digest.compute(blockBytes)
    val objectBlock =
      if (atalaReferenceLedger.supportsOnChainData) Block.BlockContent(block)
      else Block.BlockHash(ByteString.copyFrom(blockHash.value.toArray))
    val obj = node_internal.AtalaObject(block = objectBlock, blockOperationCount = 1)
    val objBytes = obj.toByteArray
    val objHash = SHA256Digest.compute(objBytes)

    val insertObject = for {
      existingObject <- AtalaObjectsDAO.get(objHash)
      _ <- {
        existingObject match {
          case Some(_) => connection.raiseError(new AtalaObjectAlreadyPublished)
          case None => AtalaObjectsDAO.insert(AtalaObjectCreateData(objHash, objBytes))
        }
      }
    } yield ()

    def storeDataOffChain(): Future[Unit] = {
      if (atalaReferenceLedger.supportsOnChainData) {
        // No need to store off-chain as whole object is in the chain already
        Future.unit
      } else {
        // Store object and block in off-chain storage
        for {
          _ <- storage.put(blockHash.hexValue, blockBytes)
          _ <- storage.put(objHash.hexValue, objBytes)
        } yield ()
      }
    }

    for {
      // Insert object into DB
      _ <- insertObject.transact(xa).unsafeToFuture()
      // If the ledger does not support data on-chain, then store it off-chain
      _ <- storeDataOffChain()
      // Publish object to the blockchain
      transactionInfo <- publishAndRecordTransaction(objHash, obj)
    } yield transactionInfo
  }

  private def publishAndRecordTransaction(
      atalaObjectId: SHA256Digest,
      atalaObject: node_internal.AtalaObject
  ): Future[TransactionInfo] = {
    for {
      // Publish object to the blockchain
      publication <- atalaReferenceLedger.publish(atalaObject)
      // Store transaction submission
      _ <-
        AtalaObjectTransactionSubmissionsDAO
          .insert(
            AtalaObjectTransactionSubmission(
              atalaObjectId,
              publication.transaction.ledger,
              publication.transaction.transactionId,
              Instant.now,
              toAtalaObjectTransactionSubmissionStatus(publication.status)
            )
          )
          .transact(xa)
          .unsafeToFuture()
    } yield publication.transaction
  }

  private def toAtalaObjectTransactionSubmissionStatus(
      status: TransactionStatus
  ): AtalaObjectTransactionSubmissionStatus = {
    status match {
      case TransactionStatus.Pending => AtalaObjectTransactionSubmissionStatus.Pending
      case TransactionStatus.InLedger => AtalaObjectTransactionSubmissionStatus.InLedger
    }
  }

  private def getProtobufObject(obj: AtalaObject): Future[node_internal.AtalaObject] = {
    val byteContentFut = obj.byteContent match {
      case Some(content) => Future.successful(content)
      case None =>
        val objectId = obj.objectId.hexValue
        storage
          .get(objectId)
          .map(_.getOrElse(throw new RuntimeException(s"Content of object $objectId not found")))
    }

    byteContentFut.map(node_internal.AtalaObject.parseFrom)
  }

  private def getBlockFromObject(obj: node_internal.AtalaObject): Future[node_internal.AtalaBlock] = {
    obj.block match {
      case node_internal.AtalaObject.Block.BlockContent(block) => Future.successful(block)
      case node_internal.AtalaObject.Block.BlockHash(hash) =>
        storage
          .get(SHA256Digest(hash.toByteArray.toVector).hexValue)
          .map(_.getOrElse(throw new RuntimeException(s"Content of block $hash not found")))
          .map(node_internal.AtalaBlock.parseFrom)
      case node_internal.AtalaObject.Block.Empty =>
        throw new IllegalStateException("Block has neither block content nor block hash")
    }
  }

  private def processObject(obj: AtalaObject): Future[ConnectionIO[Boolean]] = {
    for {
      protobufObject <- getProtobufObject(obj)
      block <- getBlockFromObject(protobufObject)
      transactionBlock =
        obj.transaction.flatMap(_.block).getOrElse(throw new RuntimeException("AtalaObject has no transaction block"))
      blockProcess = blockProcessing.processBlock(block, transactionBlock.timestamp, transactionBlock.index)
    } yield for {
      wasProcessed <- blockProcess
      _ <- AtalaObjectsDAO.setProcessed(obj.objectId)
    } yield wasProcessed
  }

  private def scheduleRetryOldPendingTransactions(delay: FiniteDuration): Unit = {
    scheduler.scheduleOnce(delay) {
      // Ensure run is scheduled after completion, even if current run fails
      retryOldPendingTransactions()
        .recover {
          case e =>
            logger.error(s"Could not retry old pending transactions", e)
            false
        }
        .onComplete { _ =>
          scheduleRetryOldPendingTransactions(config.ledgerPendingTransactionSyncDelay)
        }
    }
    ()
  }

  private[services] def retryOldPendingTransactions(): Future[Unit] = {
    for {
      // Query old pending transactions
      pendingTransactions <-
        AtalaObjectTransactionSubmissionsDAO
          .getBy(
            olderThan = Instant.now.minus(config.ledgerPendingTransactionTimeout),
            status = AtalaObjectTransactionSubmissionStatus.Pending,
            ledger = atalaReferenceLedger.getType
          )
          .transact(xa)
          .unsafeToFuture
      // Process each pending transaction
      _ <- Future.traverse(pendingTransactions) { retryTransactionIfPending }
    } yield ()
  }

  private def retryTransactionIfPending(transaction: AtalaObjectTransactionSubmission): Future[Unit] = {
    for {
      // Get current status
      transactionDetails <- atalaReferenceLedger.getTransactionDetails(transaction.transactionId)
      _ <- {
        transactionDetails.status match {
          // Transaction made it to the ledger, simply update status so it does not retry
          case TransactionStatus.InLedger =>
            AtalaObjectTransactionSubmissionsDAO
              .updateStatus(transaction.atalaObjectId, AtalaObjectTransactionSubmissionStatus.InLedger)
              .transact(xa)
              .unsafeToFuture()

          // Transaction is still pending, so it needs to be retried
          case TransactionStatus.Pending => retryTransaction(transaction)
        }
      }
    } yield ()
  }

  private def retryTransaction(transaction: AtalaObjectTransactionSubmission): Future[Unit] = {
    for {
      // Delete transaction submission and record its status in the DB
      _ <- atalaReferenceLedger.deleteTransaction(transaction.transactionId)
      _ <-
        AtalaObjectTransactionSubmissionsDAO
          .updateStatus(transaction.atalaObjectId, AtalaObjectTransactionSubmissionStatus.Deleted)
          .transact(xa)
          .unsafeToFuture()
      // Retrieve and parse object from the DB
      maybeAtalaObject <- AtalaObjectsDAO.get(transaction.atalaObjectId).transact(xa).unsafeToFuture
      atalaObject =
        maybeAtalaObject
          .flatMap(_.byteContent)
          .map(node_internal.AtalaObject.validate)
          .flatMap(_.toOption)
          .getOrElse(
            throw new RuntimeException(s"Byte contents of object ${transaction.atalaObjectId} could not be parsed")
          )
      // Publish object to the blockchain again
      _ <- publishAndRecordTransaction(transaction.atalaObjectId, atalaObject)
    } yield ()
  }
}

object ObjectManagementService {
  case class Config(
      ledgerPendingTransactionTimeout: Duration,
      ledgerPendingTransactionSyncDelay: FiniteDuration = 20.seconds
  )

  def apply(
      config: Config,
      storage: ObjectStorageService,
      atalaReferenceLedger: AtalaReferenceLedger,
      blockProcessing: BlockProcessingService
  )(implicit xa: Transactor[IO], scheduler: Scheduler): ObjectManagementService = {
    new ObjectManagementService(config, storage, atalaReferenceLedger, blockProcessing)
  }
}
