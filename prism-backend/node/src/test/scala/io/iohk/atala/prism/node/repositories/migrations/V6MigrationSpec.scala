package io.iohk.atala.prism.node.repositories.migrations

import java.time.Instant
import doobie.implicits._
import doobie.implicits.legacy.instant._
import io.iohk.atala.prism.crypto.Sha256
import io.iohk.atala.prism.repositories.PostgresMigrationSpec
import io.iohk.atala.prism.repositories.ops.SqlTestOps.Implicits

class V6MigrationSpec extends PostgresMigrationSpec("V6") {
  private val objectId = Sha256.compute("objectId".getBytes).getValue
  private val sequenceNumber = 1337
  private val objectTimestamp = Instant.now()

  private case class TestAtalaObject(
      atalaObjectId: Array[Byte],
      sequenceNumber: Int,
      objectTimestamp: Instant,
      transactionId: Array[Byte],
      ledger: String
  )

  test(
    beforeApply = {
      sql"""
                   |INSERT INTO atala_objects (atala_object_id, sequence_number, object_timestamp)
                   |VALUES($objectId, $sequenceNumber, $objectTimestamp)""".stripMargin
        .runUpdate()
    },
    afterApplied = {
      val data = sql"""
           |SELECT atala_object_id, sequence_number, object_timestamp, transaction_id, ledger
           |FROM atala_objects""".stripMargin
        .runUnique[TestAtalaObject]()

      // Verify old data is the same
      data.atalaObjectId mustBe objectId
      data.sequenceNumber mustBe sequenceNumber
      data.objectTimestamp mustBe objectTimestamp
      // Verify new data was properly set
      data.transactionId mustBe objectId
      data.ledger mustBe "InMemory"
    }
  )
}
