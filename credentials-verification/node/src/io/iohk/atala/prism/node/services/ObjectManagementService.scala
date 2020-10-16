package io.iohk.atala.prism.node.services

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
import io.iohk.atala.prism.node.repositories.daos.AtalaObjectsDAO.{AtalaObjectCreateData, AtalaObjectSetTransactionInfo}
import io.iohk.atala.prism.node.services.models.AtalaObjectNotification
import io.iohk.atala.prism.protos.node_internal.AtalaObject.Block
import io.iohk.atala.prism.protos.{node_internal, node_models}
import org.slf4j.LoggerFactory

import scala.concurrent.{ExecutionContext, Future}

private class AtalaObjectCannotBeModified extends Exception
private class AtalaObjectAlreadyPublished extends Exception

class ObjectManagementService(
    storage: ObjectStorageService,
    atalaReferenceLedger: AtalaReferenceLedger,
    blockProcessing: BlockProcessingService
)(implicit
    xa: Transactor[IO],
    ec: ExecutionContext
) {

  private val logger = LoggerFactory.getLogger(this.getClass)

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

    for {
      _ <- insertObject.transact(xa).unsafeToFuture()
      _ <- storage.put(blockHash.hexValue, blockBytes)
      _ <- storage.put(objHash.hexValue, objBytes)
      transactionInfo <- atalaReferenceLedger.publish(obj)
    } yield transactionInfo
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
          .get(SHA256Digest(hash.toByteArray.toVector).hexValue)
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
      transactionBlock =
        obj.transaction.flatMap(_.block).getOrElse(throw new RuntimeException("AtalaObject has no transaction block"))
      blockProcess = blockProcessing.processBlock(block, transactionBlock.timestamp, transactionBlock.index)
    } yield for {
      wasProcessed <- blockProcess
      _ <- AtalaObjectsDAO.setProcessed(obj.objectId)
    } yield wasProcessed
  }
}
