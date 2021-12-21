package io.iohk.atala.prism.node.repositories.daos

import cats.effect.unsafe.implicits.global
import doobie.implicits._
import io.iohk.atala.prism.AtalaWithPostgresSpec
import io.iohk.atala.prism.connector.AtalaOperationId
import io.iohk.atala.prism.node.models.{AtalaObjectId, AtalaOperationStatus}
import io.iohk.atala.prism.node.repositories.daos.AtalaObjectsDAO.AtalaObjectCreateData

class AtalaOperationsDAOSpec extends AtalaWithPostgresSpec {
  val atalaOperationId: AtalaOperationId = AtalaOperationId.random()
  val objectBytes: Array[Byte] = "random".getBytes
  val atalaObjectId: AtalaObjectId = AtalaObjectId.of(objectBytes)

  "AtalaOperationsDAO.updateAtalaOperationStatus" should {
    "update status and status details" in {
      AtalaObjectsDAO
        .insert(AtalaObjectCreateData(atalaObjectId, objectBytes))
        .transact(database)
        .unsafeRunSync()

      AtalaOperationsDAO
        .insert((atalaOperationId, atalaObjectId, AtalaOperationStatus.RECEIVED))
        .transact(database)
        .unsafeRunSync()

      AtalaOperationsDAO
        .updateAtalaOperationStatus(atalaOperationId, AtalaOperationStatus.REJECTED, "details")
        .transact(database)
        .unsafeRunSync()

      val (status, details) =
        sql"""
             |SELECT atala_operation_status,status_details
             |FROM atala_operations
             |WHERE signed_atala_operation_id = $atalaOperationId
           """.stripMargin.query[(AtalaOperationStatus, String)].option.transact(database).unsafeRunSync().get

      status must be(AtalaOperationStatus.REJECTED)
      details must be("details")
    }
  }
}
