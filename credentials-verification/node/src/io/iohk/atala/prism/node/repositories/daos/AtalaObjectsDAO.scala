package io.iohk.atala.prism.node.repositories.daos

import java.time.Instant

import doobie.free.connection.ConnectionIO
import doobie.implicits._
import io.iohk.atala.prism.crypto.SHA256Digest
import io.iohk.atala.prism.models.{Ledger, TransactionId}
import io.iohk.atala.prism.models.DoobieImplicits._
import io.iohk.atala.prism.node.models.AtalaObject

object AtalaObjectsDAO {

  case class AtalaObjectCreateData(
      objectId: SHA256Digest,
      sequenceNumber: Int,
      objectTimestamp: Instant,
      byteContent: Option[Array[Byte]] = None,
      transactionId: TransactionId,
      ledger: Ledger
  )

  def insert(data: AtalaObjectCreateData): ConnectionIO[AtalaObject] = {
    sql"""
         |INSERT INTO atala_objects
         |  (atala_object_id, sequence_number, object_timestamp, object_content, transaction_id, ledger)
         |VALUES (${data.objectId}, ${data.sequenceNumber}, ${data.objectTimestamp}, ${data.byteContent},
         |        ${data.transactionId}, ${data.ledger})
         |RETURNING atala_object_id, object_timestamp, sequence_number, object_content, transaction_id, ledger,
         |          processed
       """.stripMargin.query[AtalaObject].unique
  }

  def get(objectId: SHA256Digest): ConnectionIO[Option[AtalaObject]] = {
    sql"""
         |SELECT atala_object_id, object_timestamp, sequence_number, object_content, transaction_id, ledger, processed
         |FROM atala_objects
         |WHERE atala_object_id = $objectId
       """.stripMargin
      .query[AtalaObject]
      .option
  }

  def getNewest(): ConnectionIO[Option[AtalaObject]] = {
    sql"""
         |SELECT atala_object_id, object_timestamp, sequence_number, object_content, transaction_id, ledger, processed
         |FROM atala_objects
         |ORDER BY sequence_number DESC
         |LIMIT 1
       """.stripMargin
      .query[AtalaObject]
      .option
  }

  def setProcessed(objectId: SHA256Digest, processed: Boolean = true): ConnectionIO[Unit] = {
    sql"""
         |UPDATE atala_objects
         |SET processed = $processed
         |WHERE atala_object_id = $objectId""".stripMargin.update.run
      .map(_ => ())
  }
}
