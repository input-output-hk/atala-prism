package io.iohk.atala.prism.node.repositories.daos

import java.time.Instant

import doobie.free.connection.ConnectionIO
import doobie.implicits._
import io.iohk.cvp.crypto.SHA256Digest
import io.iohk.atala.prism.node.models.AtalaObject

object AtalaObjectsDAO {

  case class AtalaObjectCreateData(
      objectId: SHA256Digest,
      sequenceNumber: Int,
      objectTimestamp: Instant,
      byteContent: Option[Array[Byte]] = None
  )

  def insert(data: AtalaObjectCreateData): ConnectionIO[AtalaObject] = {
    sql"""
         |INSERT INTO atala_objects (atala_object_id, sequence_number, object_timestamp, object_content)
         |VALUES (${data.objectId}, ${data.sequenceNumber}, ${data.objectTimestamp}, ${data.byteContent})
         |RETURNING atala_object_id, object_timestamp, sequence_number, object_content, processed
       """.stripMargin.query[AtalaObject].unique
  }

  def get(objectId: SHA256Digest): ConnectionIO[Option[AtalaObject]] = {
    sql"""
         |SELECT atala_object_id, object_timestamp, sequence_number, object_content, processed
         |FROM atala_objects
         |WHERE atala_object_id = $objectId
       """.stripMargin
      .query[AtalaObject]
      .option
  }

  def getNewest(): ConnectionIO[Option[AtalaObject]] = {
    sql"""
         |SELECT atala_object_id, object_timestamp, sequence_number, object_content, processed
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
