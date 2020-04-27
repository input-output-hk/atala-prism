package io.iohk.node.services

import java.time.Instant

import cats.effect.IO
import com.google.protobuf.ByteString
import doobie.free.connection
import doobie.free.connection.ConnectionIO
import doobie.implicits._
import doobie.util.transactor.Transactor
import io.iohk.cvp.crypto.SHA256Digest
import io.iohk.node.AtalaReferenceLedger
import io.iohk.node.models.AtalaObject
import io.iohk.node.objects.ObjectStorageService
import io.iohk.node.repositories.daos.AtalaObjectsDAO
import io.iohk.node.repositories.daos.AtalaObjectsDAO.AtalaObjectCreateData
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
  def justSaveReference(ref: SHA256Digest, timestamp: Instant): Future[Option[AtalaObject]] = {
    val query = for {
      existingObject <- AtalaObjectsDAO.get(ref)
      _ <- {
        existingObject match {
          case Some(_) => connection.raiseError(new ReferenceAlreadyProcessed)
          case None => connection.unit
        }
      }

      newestObject <- AtalaObjectsDAO.getNewest()
      obj <- AtalaObjectsDAO.insert(
        AtalaObjectCreateData(ref, newestObject.fold(INITIAL_SEQUENCE_NUMBER)(_.sequenceNumber + 1), timestamp)
      )
    } yield Some(obj)

    query
      .transact(xa)
      .unsafeToFuture()
      .recover {
        case _: ReferenceAlreadyProcessed => None
      }
  }

  def saveReference(ref: SHA256Digest, timestamp: Instant): Future[Unit] = {
    // TODO: just add the object to processing queue, instead of processing here
    justSaveReference(ref, timestamp)
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
    val obj = node_internal.AtalaObject(blockHash = ByteString.copyFrom(blockHash.value), blockOperationCount = 1)
    val objBytes = obj.toByteArray
    val objHash = SHA256Digest.compute(objBytes)

    storage.put(blockHash.hexValue, blockBytes)
    storage.put(objHash.hexValue, objBytes)

    synchronizer.publishReference(objHash)
  }

  protected def processObject(obj: AtalaObject): Future[ConnectionIO[Boolean]] = {
    for {
      blockHash <- obj.blockHash match {
        case Some(blockHash) =>
          Future.successful(blockHash)
        case None =>
          val objectFileName = obj.objectId.hexValue
          for {
            objectBytes <- storage.get(objectFileName).map(_.get) // TODO: error support
            aobject = node_internal.AtalaObject.parseFrom(objectBytes)
            blockHash = SHA256Digest(aobject.blockHash.toByteArray)
            _ <-
              AtalaObjectsDAO
                .setBlockHash(obj.objectId, blockHash)
                .transact(xa)
                .unsafeToFuture()
          } yield blockHash
      }
      blockTransaction <- processBlock(blockHash, obj.objectTimestamp, obj.sequenceNumber)
    } yield for {
      result <- blockTransaction
      _ <- AtalaObjectsDAO.setProcessed(obj.objectId)
    } yield result
  }

  protected def processBlock(
      hash: SHA256Digest,
      blockTimestamp: Instant,
      blockSequenceNumber: Int
  ): Future[ConnectionIO[Boolean]] = {
    val blockFileName = hash.hexValue
    for {
      blockBytes <- storage.get(blockFileName).map(_.get) // TODO: error support
      block = node_internal.AtalaBlock.parseFrom(blockBytes)
      transaction = blockProcessing.processBlock(block, blockTimestamp, blockSequenceNumber)
    } yield transaction
  }
}
