package io.iohk.atala.prism.node.repositories.daos

import doobie.free.connection
import doobie.free.connection.ConnectionIO
import doobie.implicits._
import doobie.implicits.legacy.instant._
import io.iohk.atala.prism.models.DoobieImplicits._
import io.iohk.atala.prism.models.TransactionInfo
import io.iohk.atala.prism.node.models.{AtalaObject, AtalaObjectId}

object AtalaObjectsDAO {

  case class AtalaObjectCreateData(objectId: AtalaObjectId, byteContent: Array[Byte])
  case class AtalaObjectSetTransactionInfo(objectId: AtalaObjectId, transactionInfo: TransactionInfo)

  def insert(data: AtalaObjectCreateData): ConnectionIO[Unit] = {
    sql"""
         |INSERT INTO atala_objects (atala_object_id, object_content)
         |VALUES (${data.objectId}, ${data.byteContent})
       """.stripMargin.update.run.map(_ => ())
  }

  def setTransactionInfo(data: AtalaObjectSetTransactionInfo): ConnectionIO[Unit] = {
    val transaction = data.transactionInfo
    transaction.block match {
      case Some(block) =>
        sql"""
             |INSERT INTO atala_object_txs (atala_object_id, ledger, block_number, block_index, block_timestamp, transaction_id)
             |VALUES (${data.objectId}, ${transaction.ledger}, ${block.number}, ${block.index}, ${block.timestamp}, ${transaction.transactionId})
           """.stripMargin.update.run.map(_ => ())
      case _ => connection.raiseError(new IllegalArgumentException("Transaction has bo block"))
    }
  }

  def get(objectId: AtalaObjectId): ConnectionIO[Option[AtalaObject]] = {
    sql"""
         |SELECT obj.atala_object_id, obj.object_content, obj.processed,
         |       tx.transaction_id, tx.ledger, tx.block_number, tx.block_timestamp, tx.block_index
         |FROM atala_objects AS obj
         |  LEFT OUTER JOIN atala_object_txs AS tx ON tx.atala_object_id = obj.atala_object_id
         |WHERE obj.atala_object_id = $objectId
       """.stripMargin
      .query[AtalaObject]
      .option
  }

  def setProcessed(objectId: AtalaObjectId, processed: Boolean = true): ConnectionIO[Unit] = {
    sql"""
         |UPDATE atala_objects
         |SET processed = $processed
         |WHERE atala_object_id = $objectId""".stripMargin.update.run
      .map(_ => ())
  }
}
