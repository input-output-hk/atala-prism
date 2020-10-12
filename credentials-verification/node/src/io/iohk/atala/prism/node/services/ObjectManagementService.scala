package io.iohk.atala.prism.node.services

import java.time.Instant

import cats.effect.IO
import com.google.protobuf.ByteString
import doobie.free.connection
import doobie.free.connection.ConnectionIO
import doobie.implicits._
import doobie.util.transactor.Transactor
import io.iohk.atala.prism.crypto.SHA256Digest
import io.iohk.atala.prism.models.TransactionInfo
import io.iohk.atala.prism.node.AtalaReferenceLedger
import io.iohk.atala.prism.node.models.AtalaObject
import io.iohk.atala.prism.node.objects.ObjectStorageService
import io.iohk.atala.prism.node.repositories.daos.AtalaObjectsDAO
import io.iohk.atala.prism.node.repositories.daos.AtalaObjectsDAO.AtalaObjectCreateData
import io.iohk.atala.prism.node.services.models.AtalaObjectNotification
import io.iohk.prism.protos.node_internal.AtalaObject.Block
import io.iohk.prism.protos.{node_internal, node_models}
import org.slf4j.LoggerFactory

import scala.concurrent.{ExecutionContext, Future}

class ReferenceAlreadyProcessed extends Exception

object ObjectManagementService {
  val INITIAL_SEQUENCE_NUMBER = 1
}

class ObjectManagementService(
    storage: ObjectStorageService,
    atalaReferenceLedger: AtalaReferenceLedger,
    blockProcessing: BlockProcessingService
)(implicit
    xa: Transactor[IO],
    ec: ExecutionContext
) {

  import ObjectManagementService._

  private val logger = LoggerFactory.getLogger(this.getClass)

  private def justSaveObject(notification: AtalaObjectNotification): Future[Option[AtalaObject]] = {
    val objectBytes = notification.atalaObject.toByteArray
    val hash = SHA256Digest.compute(objectBytes)

    val query = for {
      existingObject <- AtalaObjectsDAO.get(hash)
      _ <- {
        existingObject match {
          case Some(_) => connection.raiseError(new ReferenceAlreadyProcessed)
          case None => connection.unit
        }
      }

      newestObject <- AtalaObjectsDAO.getNewest()
      block = notification.transaction.block.getOrElse(
        throw new IllegalArgumentException("Transaction has no block")
      )
      obj <- AtalaObjectsDAO.insert(
        AtalaObjectCreateData(
          hash,
          newestObject.fold(INITIAL_SEQUENCE_NUMBER)(_.sequenceNumber + 1),
          block.timestamp,
          Some(objectBytes),
          notification.transaction.transactionId,
          notification.transaction.ledger
        )
      )
    } yield Some(obj)

    query
      .transact(xa)
      .unsafeToFuture()
      .recover {
        case _: ReferenceAlreadyProcessed => None
      }
  }

  def saveObject(notification: AtalaObjectNotification): Future[Unit] = {
    // TODO: just add the object to processing queue, instead of processing here
    justSaveObject(notification)
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
      else Block.BlockHash(ByteString.copyFrom(blockHash.value))
    val obj =
      node_internal.AtalaObject(block = objectBlock, blockOperationCount = 1)
    val objBytes = obj.toByteArray
    val objHash = SHA256Digest.compute(objBytes)

    storage.put(blockHash.hexValue, blockBytes)
    storage.put(objHash.hexValue, objBytes)

    atalaReferenceLedger.publish(obj)
  }

  protected def getProtobufObject(obj: AtalaObject): Future[node_internal.AtalaObject] = {
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

  protected def getBlockFromObject(obj: node_internal.AtalaObject): Future[node_internal.AtalaBlock] = {
    obj.block match {
      case node_internal.AtalaObject.Block.BlockContent(block) => Future.successful(block)
      case node_internal.AtalaObject.Block.BlockHash(hash) =>
        storage
          .get(SHA256Digest(hash.toByteArray).hexValue)
          .map(_.getOrElse(throw new RuntimeException(s"Content of block $hash not found")))
          .map(node_internal.AtalaBlock.parseFrom)
      case node_internal.AtalaObject.Block.Empty =>
        throw new IllegalStateException("Block has neither block content nor block hash")
    }
  }

  protected def processObject(obj: AtalaObject): Future[ConnectionIO[Boolean]] = {
    for {
      protobufObject <- getProtobufObject(obj)
      block <- getBlockFromObject(protobufObject)
      blockTransaction = processBlock(block, obj.objectTimestamp, obj.sequenceNumber)
    } yield for {
      result <- blockTransaction
      _ <- AtalaObjectsDAO.setProcessed(obj.objectId)
    } yield result
  }

  protected def processBlock(
      block: node_internal.AtalaBlock,
      blockTimestamp: Instant,
      blockSequenceNumber: Int
  ): ConnectionIO[Boolean] = {
    blockProcessing.processBlock(block, blockTimestamp, blockSequenceNumber)
  }
}
