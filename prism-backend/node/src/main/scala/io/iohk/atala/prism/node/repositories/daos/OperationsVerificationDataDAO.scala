package io.iohk.atala.prism.node.repositories.daos

import cats.syntax.functor._
import doobie.free.connection.ConnectionIO
import doobie.implicits._
import io.iohk.atala.prism.kotlin.crypto.Sha256Digest
import io.iohk.atala.prism.models.DidSuffix

object OperationsVerificationDataDAO {
  def insert(
      previousOperation: Option[Sha256Digest],
      signedWithDidId: DidSuffix,
      signedWithKeyId: String
  ): ConnectionIO[Unit] = {
    sql"""
         |INSERT INTO operations_verification_data (previous_operation, signed_with_did_id, signed_with_key_id)
         |VALUES ($previousOperation, $signedWithDidId, $signedWithKeyId)
       """.stripMargin.update.run.void
  }

  def countPreviousOperation(
      previousOperation: Sha256Digest
  ): ConnectionIO[Int] = {
    sql"""
         |SELECT COUNT(*)
         |FROM operations_verification_data
         |WHERE previous_operation = $previousOperation
       """.stripMargin.query[Int].unique
  }

  def countSignedWithKeys(
      signedWithDidId: DidSuffix,
      signedWithKeyId: String
  ): ConnectionIO[Int] = {
    sql"""
         |SELECT COUNT(*)
         |FROM operations_verification_data
         |WHERE signed_with_did_id = $signedWithDidId and signed_with_key_id = $signedWithKeyId
       """.stripMargin.query[Int].unique
  }

  def countDidIds(didId: DidSuffix): ConnectionIO[Int] = {
    sql"""
         |SELECT COUNT(*)
         |FROM operations_verification_data
         |WHERE signed_with_did_id = $didId
       """.stripMargin.query[Int].unique
  }
}
