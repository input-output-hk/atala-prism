package io.iohk.atala.prism.node.repositories.daos

import cats.data.NonEmptyList
import cats.syntax.functor._
import doobie.free.connection
import doobie.free.connection.{ConnectionIO, unit}
import doobie.implicits._
import doobie.implicits.legacy.instant._
import doobie.util.fragments.in
import io.iohk.atala.prism.models.TransactionInfo
import io.iohk.atala.prism.node.models.{AtalaObjectId, AtalaObjectInfo, AtalaObjectStatus}

import java.time.Instant

object AtalaObjectsDAO {

  case class AtalaObjectCreateData(
      objectId: AtalaObjectId,
      byteContent: Array[Byte],
      status: AtalaObjectStatus
  )
  case class AtalaObjectSetTransactionInfo(
      objectId: AtalaObjectId,
      transactionInfo: TransactionInfo
  )

  def insert(data: AtalaObjectCreateData): ConnectionIO[Int] = {
    sql"""
         |INSERT INTO atala_objects (atala_object_id, object_content, atala_object_status, received_at)
         |VALUES (${data.objectId}, ${data.byteContent}, ${data.status}, ${Instant
        .now()})
         |ON CONFLICT (atala_object_id) DO NOTHING
       """.stripMargin.update.run
  }

  def setTransactionInfo(
      data: AtalaObjectSetTransactionInfo
  ): ConnectionIO[Unit] = {
    val transaction = data.transactionInfo
    transaction.block match {
      case Some(block) =>
        sql"""
             |INSERT INTO atala_object_txs (atala_object_id, ledger, block_number, block_index, block_timestamp, transaction_id)
             |VALUES (${data.objectId}, ${transaction.ledger}, ${block.number}, ${block.index}, ${block.timestamp}, ${transaction.transactionId})
           """.stripMargin.update.run.void
      case _ =>
        connection.raiseError(
          new IllegalArgumentException("Transaction has bo block")
        )
    }
  }

  def get(objectId: AtalaObjectId): ConnectionIO[Option[AtalaObjectInfo]] = {
    sql"""
         |SELECT obj.atala_object_id, obj.object_content, obj.atala_object_status,
         |       tx.transaction_id, tx.ledger, tx.block_number, tx.block_timestamp, tx.block_index
         |FROM atala_objects AS obj
         |  LEFT OUTER JOIN atala_object_txs AS tx ON tx.atala_object_id = obj.atala_object_id
         |WHERE obj.atala_object_id = $objectId
       """.stripMargin
      .query[AtalaObjectInfo]
      .option
  }

  def getNotPublishedObjectInfos: ConnectionIO[List[AtalaObjectInfo]] = {
    sql"""
         |SELECT obj.atala_object_id, obj.object_content, obj.atala_object_status,
         |       tx.transaction_id, tx.ledger, tx.block_number, tx.block_timestamp, tx.block_index
         |FROM
         |(
         |  SELECT *
         |  FROM atala_objects AS obj
         |  WHERE atala_object_status = 'PENDING' and NOT EXISTS
         |  (
         |    SELECT 1
         |      FROM atala_object_tx_submissions
         |      WHERE atala_object_id = obj.atala_object_id AND status != 'DELETED'
         |  )
         |) as obj
         |  LEFT OUTER JOIN atala_object_txs AS tx ON tx.atala_object_id = obj.atala_object_id
         |ORDER BY obj.received_at ASC;
       """.stripMargin
      .query[AtalaObjectInfo]
      .to[List]
  }

  def getNotProcessedAtalaObjects: ConnectionIO[List[AtalaObjectInfo]] = {
    sql"""
         |SELECT obj.atala_object_id, obj.object_content, obj.atala_object_status,
         |       tx.transaction_id, tx.ledger, tx.block_number, tx.block_timestamp, tx.block_index
         |FROM atala_objects AS obj
         |LEFT OUTER JOIN atala_object_txs AS tx ON tx.atala_object_id = obj.atala_object_id
         |WHERE obj.atala_object_status = 'SCHEDULED' or obj.atala_object_status = 'PENDING'
         |ORDER BY obj.received_at ASC;
       """.stripMargin
      .query[AtalaObjectInfo]
      .to[List]
  }

  def updateObjectStatus(
      objectId: AtalaObjectId,
      newStatus: AtalaObjectStatus
  ): ConnectionIO[Unit] = {
    sql"""
         |UPDATE atala_objects
         |SET atala_object_status = $newStatus
         |WHERE atala_object_id = $objectId
      """.stripMargin.update.run.void
  }

  def updateObjectStatus(oldObjectStatus: AtalaObjectStatus, newObjectStatus: AtalaObjectStatus): ConnectionIO[Int] =
    sql"""
         |UPDATE atala_objects
         |SET atala_object_status = $newObjectStatus
         |WHERE atala_object_status = $oldObjectStatus
       """.stripMargin.update.run

  def updateObjectStatusBatch(
      objectIds: List[AtalaObjectId],
      newStatus: AtalaObjectStatus
  ): ConnectionIO[Unit] = {
    NonEmptyList.fromList(objectIds).fold(unit) { objectIdsNonEmpty =>
      val fragment = fr"UPDATE atala_objects" ++
        fr"SET atala_object_status = $newStatus" ++
        fr"WHERE" ++ in(fr"atala_object_id", objectIdsNonEmpty)
      fragment.update.run.void
    }
  }
}
