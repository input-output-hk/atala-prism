package io.iohk.node.repositories.daos

import doobie.free.connection.ConnectionIO
import doobie.implicits._
import io.iohk.node.models.{AtalaObject, SHA256Digest}

object AtalaObjectsDAO {

  case class AtalaObjectCreateData(
      objectId: SHA256Digest,
      sequenceNumber: Int
  )

  def insert(data: AtalaObjectCreateData): ConnectionIO[AtalaObject] = {
    sql"""
         |INSERT INTO atala_objects (atala_object_id, sequence_number)
         |VALUES (${data.objectId}, ${data.sequenceNumber})
         |RETURNING atala_object_id, sequence_number, atala_block_hash, processed
       """.stripMargin.query[AtalaObject].unique
  }

  def get(objectId: SHA256Digest): ConnectionIO[Option[AtalaObject]] = {
    sql"""
         |SELECT atala_object_id, sequence_number, atala_block_hash, processed
         |FROM atala_objects
         |WHERE atala_object_id = $objectId
       """.stripMargin
      .query[AtalaObject]
      .option
  }

  def getNewest(): ConnectionIO[Option[AtalaObject]] = {
    sql"""
         |SELECT atala_object_id, sequence_number, atala_block_hash, processed
         |FROM atala_objects
         |ORDER BY sequence_number DESC
         |LIMIT 1
       """.stripMargin
      .query[AtalaObject]
      .option
  }

  def setBlockHash(objectId: SHA256Digest, blockHash: SHA256Digest): ConnectionIO[Unit] = {
    sql"""
         |UPDATE atala_objects
         |SET atala_block_hash = $blockHash
         |WHERE atala_object_id = $objectId""".stripMargin.update.run
      .map(_ => ())
  }

  def setProcessed(objectId: SHA256Digest, processed: Boolean = true): ConnectionIO[Unit] = {
    sql"""
         |UPDATE atala_objects
         |SET processed = $processed
         |WHERE atala_object_id = $objectId""".stripMargin.update.run
      .map(_ => ())
  }
}
