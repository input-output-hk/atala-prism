package io.iohk.atala.prism.node.repositories.daos

import cats.data.NonEmptyList
import cats.syntax.functor._
import doobie.Fragments.in
import doobie.free.connection.{ConnectionIO, unit}
import doobie.implicits._
import doobie.util.update.Update
import io.iohk.atala.prism.node.models.AtalaOperationId
import io.iohk.atala.prism.node.models.{AtalaObjectId, AtalaOperationInfo, AtalaOperationStatus}

object AtalaOperationsDAO {
  type AtalaOperationData =
    (AtalaOperationId, AtalaObjectId, AtalaOperationStatus)

  private val insertSQL =
    """
      |INSERT INTO atala_operations (signed_atala_operation_id, atala_object_id, atala_operation_status)
      |VALUES (?, ?, ?)
      |ON CONFLICT (signed_atala_operation_id) DO NOTHING
    """.stripMargin

  def insert(data: AtalaOperationData): ConnectionIO[Int] = {
    Update[AtalaOperationData](insertSQL).run(data)
  }

  def insertMany(dataMany: List[AtalaOperationData]): ConnectionIO[Int] = {
    Update[AtalaOperationData](insertSQL).updateMany(dataMany)
  }

  def getAtalaOperationsCount(status: AtalaOperationStatus): ConnectionIO[Int] =
    sql"""
         |SELECT COUNT(*)
         |FROM atala_operations
         |WHERE atala_operation_status = $status
       """.stripMargin.query[Int].unique

  def updateAtalaOperationStatus(
      atalaOperationId: AtalaOperationId,
      atalaOperationStatus: AtalaOperationStatus,
      statusDetails: String = ""
  ): ConnectionIO[Unit] = {
    sql"""
         |UPDATE atala_operations
         |SET atala_operation_status = $atalaOperationStatus,
         |    status_details = $statusDetails
         |WHERE signed_atala_operation_id = $atalaOperationId""".stripMargin.update.run.void
  }

  def updateAtalaOperationStatusBatch(
      atalaOperationIds: List[AtalaOperationId],
      atalaOperationStatus: AtalaOperationStatus
  ): ConnectionIO[Unit] = {
    NonEmptyList.fromList(atalaOperationIds).fold(unit) { atalaOperationIdsNonEmpty =>
      val fragment = fr"UPDATE atala_operations" ++
        fr"SET atala_operation_status = $atalaOperationStatus" ++
        fr"WHERE" ++ in(
          fr"signed_atala_operation_id",
          atalaOperationIdsNonEmpty
        )
      fragment.update.run.void
    }
  }

  def updateAtalaOperationObjectBatch(
      atalaOperationIds: List[AtalaOperationId],
      atalaObjectId: AtalaObjectId
  ): ConnectionIO[Unit] = {
    NonEmptyList.fromList(atalaOperationIds).fold(unit) { atalaOperationIdsNonEmpty =>
      val fragment = fr"UPDATE atala_operations" ++
        fr"SET atala_object_id = $atalaObjectId" ++
        fr"WHERE" ++ in(
          fr"signed_atala_operation_id",
          atalaOperationIdsNonEmpty
        )
      fragment.update.run.void
    }
  }

  def getAtalaOperationInfo(
      atalaOperationId: AtalaOperationId
  ): ConnectionIO[Option[AtalaOperationInfo]] = {
    sql"""
         |SELECT ops.signed_atala_operation_id, ops.atala_object_id, ops.atala_operation_status, ops.status_details, tx.status, tx.transaction_id
         |FROM atala_operations as ops
         |  LEFT OUTER JOIN atala_object_tx_submissions AS tx ON tx.atala_object_id = ops.atala_object_id
         |WHERE signed_atala_operation_id = $atalaOperationId
       """.stripMargin.query[AtalaOperationInfo].option
  }
}
