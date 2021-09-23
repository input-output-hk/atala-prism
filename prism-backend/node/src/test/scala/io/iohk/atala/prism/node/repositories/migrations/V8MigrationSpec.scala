package io.iohk.atala.prism.node.repositories.migrations

import java.time.Instant
import doobie.implicits._
import doobie.implicits.legacy.instant._
import io.iohk.atala.prism.crypto.Sha256
import io.iohk.atala.prism.repositories.PostgresMigrationSpec
import io.iohk.atala.prism.repositories.ops.SqlTestOps.Implicits

class V8MigrationSpec extends PostgresMigrationSpec("V8") {
  private val objectId = Sha256.compute("objectId".getBytes).getValue
  private val objectContent = "objectContent".getBytes
  private val sequenceNumber = 1337
  private val objectTimestamp = Instant.now()
  private val transactionId = Sha256.compute("transactionId".getBytes).getValue
  private val ledger = "SomeLedger"

  private case class TestAtalaObject(
      atalaObjectId: Array[Byte],
      objectContent: Array[Byte]
  )

  private case class TestAtalaObjectTx(
      atalaObjectId: Array[Byte],
      ledger: String,
      blockNumber: Int,
      blockTimestamp: Instant,
      blockIndex: Int,
      transactionId: Array[Byte]
  )

  test(
    beforeApply = {
      sql"""
           |INSERT INTO atala_objects
           |  (atala_object_id, object_content, sequence_number, object_timestamp, transaction_id, ledger)
           |VALUES ($objectId, $objectContent, $sequenceNumber, $objectTimestamp, $transactionId, $ledger)
         """.stripMargin.runUpdate()
    },
    afterApplied = {
      val atalaObject = sql"""
                      |SELECT atala_object_id, object_content
                      |FROM atala_objects
                    """.stripMargin.runUnique[TestAtalaObject]()
      val atalaObjectTx = sql"""
                             |SELECT atala_object_id, ledger, block_number, block_timestamp, block_index, transaction_id
                             |FROM atala_object_txs
                    """.stripMargin.runUnique[TestAtalaObjectTx]()

      // Verify old data is the same
      atalaObject.atalaObjectId mustBe objectId
      atalaObject.objectContent mustBe objectContent
      // Verify new data was properly set
      atalaObjectTx.atalaObjectId mustBe objectId
      atalaObjectTx.ledger mustBe ledger
      atalaObjectTx.blockNumber mustBe 1
      atalaObjectTx.blockTimestamp mustBe objectTimestamp
      atalaObjectTx.blockIndex mustBe sequenceNumber
      atalaObjectTx.transactionId mustBe transactionId
    }
  )
}
