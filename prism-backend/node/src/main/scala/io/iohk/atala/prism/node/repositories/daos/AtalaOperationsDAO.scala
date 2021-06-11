package io.iohk.atala.prism.node.repositories.daos

import cats.data.NonEmptyList
import cats.syntax.functor._
import doobie.Fragments.in
import doobie.free.connection.{ConnectionIO, unit}
import doobie.implicits._
import doobie.util.update.Update
import io.iohk.atala.prism.node.models.{AtalaObjectId, AtalaOperationId, AtalaOperationInfo, AtalaOperationStatus}
import io.iohk.atala.prism.node.repositories.daos._

object AtalaOperationsDAO {
  type AtalaOperationData = (AtalaOperationId, AtalaObjectId, AtalaOperationStatus)

  private val insertSQL =
    "INSERT INTO atala_operations (signed_atala_operation_id, atala_object_id, atala_operation_status) VALUES (?, ?, ?)"

  def insert(data: AtalaOperationData): ConnectionIO[Unit] = {
    Update[AtalaOperationData](insertSQL).run(data).void
  }

  def insertMany(dataMany: List[AtalaOperationData]): ConnectionIO[Unit] = {
    Update[AtalaOperationData](insertSQL).updateMany(dataMany).void
  }

  def updateAtalaOperationStatus(
      atalaOperationId: AtalaOperationId,
      atalaOperationStatus: AtalaOperationStatus
  ): ConnectionIO[Unit] = {
    sql"""
         |UPDATE atala_operations
         |SET atala_operation_status = $atalaOperationStatus
         |WHERE signed_atala_operation_id = $atalaOperationId""".stripMargin.update.run.void
  }

  def updateAtalaOperationStatusBatch(
      atalaOperationIds: List[AtalaOperationId],
      atalaOperationStatus: AtalaOperationStatus
  ): ConnectionIO[Unit] = {
    NonEmptyList.fromList(atalaOperationIds) match {
      case Some(atalaOperationIdsNonEmpty) =>
        val fragment = fr"UPDATE atala_operations" ++
          fr"SET atala_operation_status = $atalaOperationStatus" ++
          fr"WHERE" ++ in(fr"signed_atala_operation_id", atalaOperationIdsNonEmpty)
        fragment.update.run.void
      case None =>
        unit
    }
  }

  def getAtalaOperationInfo(atalaOperationId: AtalaOperationId): ConnectionIO[Option[AtalaOperationInfo]] = {
    sql"""
         |SELECT ops.signed_atala_operation_id, ops.atala_object_id, ops.atala_operation_status, tx.status
         |FROM atala_operations as ops
         |  LEFT OUTER JOIN atala_object_tx_submissions AS tx ON tx.atala_object_id = ops.atala_object_id
         |WHERE signed_atala_operation_id = $atalaOperationId
       """.stripMargin.query[AtalaOperationInfo].option
  }
}
