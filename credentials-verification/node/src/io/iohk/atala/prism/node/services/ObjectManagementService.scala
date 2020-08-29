package io.iohk.atala.prism.node.services

import java.time.Instant

import cats.effect.IO
import com.google.protobuf.ByteString
import doobie.free.connection
import doobie.free.connection.ConnectionIO
import doobie.implicits._
import doobie.util.transactor.Transactor
import io.iohk.atala.prism.crypto.SHA256Digest
import io.iohk.atala.prism.node.AtalaReferenceLedger
import io.iohk.atala.prism.node.models.AtalaObject
import io.iohk.atala.prism.node.objects.ObjectStorageService
import io.iohk.atala.prism.node.repositories.daos.AtalaObjectsDAO
import io.iohk.atala.prism.node.repositories.daos.AtalaObjectsDAO.AtalaObjectCreateData
import io.iohk.atala.prism.node.services.models.AtalaObjectUpdate
import io.iohk.prism.protos.node_internal.AtalaObject.Block
import io.iohk.prism.protos.{node_internal, node_models}

import scala.concurrent.{ExecutionContext, Future}

class ReferenceAlreadyProcessed extends Exception

object ObjectManagementService {
  val INITIAL_SEQUENCE_NUMBER = 1
}

class ObjectManagementService(
    storage: ObjectStorageService,
    synchronizer: AtalaReferenceLedger,
    blockProcessing: BlockProcessingService
)(implicit
    xa: Transactor[IO],
    ec: ExecutionContext
) {

  import ObjectManagementService._

  // method `saveReference` should eventually do what just `justSaveReference` does
  // - put the info about the object into the db
  // for now, until we have block processing queue, it also manages processing
  // the referenced block
  def justSaveObject(objectUpdate: AtalaObjectUpdate, timestamp: Instant): Future[Option[AtalaObject]] = {
    val hash = objectUpdate match {
      case AtalaObjectUpdate.Reference(ref) => ref
      case AtalaObjectUpdate.ByteContent(bytes) => SHA256Digest.compute(bytes)
    }

    val content = objectUpdate match {
      case AtalaObjectUpdate.Reference(_) => None
      case AtalaObjectUpdate.ByteContent(bytes) => Some(bytes)
    }

    val query = for {
      existingObject <- AtalaObjectsDAO.get(hash)
      _ <- {
        existingObject match {
          case Some(_) => connection.raiseError(new ReferenceAlreadyProcessed)
          case None => connection.unit
        }
      }

      newestObject <- AtalaObjectsDAO.getNewest()
      obj <- AtalaObjectsDAO.insert(
        AtalaObjectCreateData(
          hash,
          newestObject.fold(INITIAL_SEQUENCE_NUMBER)(_.sequenceNumber + 1),
          timestamp,
          content
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

  def saveObject(obj: AtalaObjectUpdate, timestamp: Instant): Future[Unit] = {
    // TODO: just add the object to processing queue, instead of processing here
    justSaveObject(obj, timestamp)
      .flatMap {
        case Some(obj) =>
          processObject(obj).flatMap { transaction =>
            transaction.transact(xa).unsafeToFuture().map(_ => ())
          }
        case None =>
          Future.successful(())
      }
  }

  def publishAtalaOperation(op: node_models.SignedAtalaOperation): Future[Unit] = {
    val block = node_internal.AtalaBlock("1.0", List(op))
    val blockBytes = block.toByteArray
    val blockHash = SHA256Digest.compute(blockBytes)
    val obj =
      node_internal.AtalaObject(block = Block.BlockHash(ByteString.copyFrom(blockHash.value)), blockOperationCount = 1)
    val objBytes = obj.toByteArray
    val objHash = SHA256Digest.compute(objBytes)

    storage.put(blockHash.hexValue, blockBytes)
    storage.put(objHash.hexValue, objBytes)

    synchronizer.publishReference(objHash)
  }

  protected def getProtobufObject(obj: AtalaObject): Future[node_internal.AtalaObject] = {
    val byteContentFut = obj.byteContent match {
      case Some(content) => Future.successful(content)
      case None => storage.get(obj.objectId.hexValue).map(_.get) // TODO: error support
    }

    byteContentFut.map(node_internal.AtalaObject.parseFrom)
  }

  protected def getBlockFromObject(obj: node_internal.AtalaObject): Future[node_internal.AtalaBlock] = {
    obj.block match {
      case node_internal.AtalaObject.Block.BlockContent(block) => Future.successful(block)
      case node_internal.AtalaObject.Block.BlockHash(hash) =>
        storage
          .get(SHA256Digest(hash.toByteArray).hexValue)
          .map(_.get) // TODO: error support
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
