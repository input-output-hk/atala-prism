package io.iohk.node.services

import cats.effect.IO
import com.google.protobuf.ByteString
import doobie.free.connection
import doobie.free.connection.ConnectionIO
import doobie.implicits._
import doobie.util.transactor.Transactor
import io.iohk.node.models.{AtalaObject, SHA256Digest}
import io.iohk.node.objects.ObjectStorageService
import io.iohk.node.repositories.daos.AtalaObjectsDAO
import io.iohk.node.repositories.daos.AtalaObjectsDAO.AtalaObjectCreateData
import io.iohk.node.{AtalaReferenceLedger, atala_bitcoin => atala_proto, geud_node => geud_proto}

import scala.concurrent.{ExecutionContext, Future}

class ReferenceAlreadyProcessed extends Exception

object ObjectManagementService {
  val INITIAL_SEQUENCE_NUMBER = 1
}

class ObjectManagementService(
    storage: ObjectStorageService,
    synchronizer: AtalaReferenceLedger,
    blockProcessing: BlockProcessingService
)(
    implicit xa: Transactor[IO],
    ec: ExecutionContext
) {

  import ObjectManagementService._

  // method `saveReference` should eventually do what just `justSaveReference` does
  // - put the info about the object into the db
  // for now, until we have block processing queue, it also manages processing
  // the referenced block
  def justSaveReference(ref: SHA256Digest): Future[Option[AtalaObject]] = {
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
        AtalaObjectCreateData(ref, newestObject.fold(INITIAL_SEQUENCE_NUMBER)(_.sequenceNumber + 1))
      )
    } yield Some(obj)

    query
      .transact(xa)
      .unsafeToFuture()
      .recover {
        case _: ReferenceAlreadyProcessed => None
      }
  }

  def saveReference(ref: SHA256Digest): Future[Unit] = {
    // TODO: just add the object to processing queue, instead of processing here
    justSaveReference(ref)
      .flatMap {
        case Some(obj) =>
          processObject(obj).transact(xa).unsafeToFuture().map(_ => ())
        case None =>
          Future.successful(())
      }
  }

  def publishAtalaOperation(op: geud_proto.SignedAtalaOperation): Future[Unit] = {
    val block = atala_proto.AtalaBlock("1.0", List(op))
    val blockBytes = block.toByteArray
    val blockHash = SHA256Digest.compute(blockBytes)
    val obj = atala_proto.AtalaObject(blockHash = ByteString.copyFrom(blockHash.value), blockOperationCount = 1)
    val objBytes = obj.toByteArray
    val objHash = SHA256Digest.compute(objBytes)

    storage.put(blockHash.hexValue, blockBytes)
    storage.put(objHash.hexValue, objBytes)

    synchronizer.publishReference(objHash)
  }

  protected def processObject(obj: AtalaObject): ConnectionIO[Boolean] = {
    for {
      blockHash <- obj.blockHash match {
        case Some(hash) =>
          connection.pure(hash)
        case None =>
          val objectFileName = obj.objectId.hexValue
          val objectBytes = storage.get(objectFileName).get // TODO: error support
          val aobject = atala_proto.AtalaObject.parseFrom(objectBytes)
          val blockHash = SHA256Digest(aobject.blockHash.toByteArray)
          AtalaObjectsDAO.setBlockHash(obj.objectId, blockHash).map(_ => blockHash)
      }
      res <- processBlock(blockHash)
      _ <- AtalaObjectsDAO.setProcessed(obj.objectId)
    } yield res
  }

  protected def processBlock(hash: SHA256Digest): ConnectionIO[Boolean] = {
    val blockFileName = hash.hexValue
    val blockBytes = storage.get(blockFileName).get // TODO: error support
    val block = atala_proto.AtalaBlock.parseFrom(blockBytes)
    blockProcessing.processBlock(block)
  }
}
