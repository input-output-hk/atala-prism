package io.iohk.atala.prism.node.repositories.daos

import cats.syntax.functor._
import doobie.free.connection.ConnectionIO
import doobie.implicits._
import io.iohk.atala.prism.models.DidSuffix

object RevokedKeysDAO {
  def insert(
      signedWithDidId: DidSuffix,
      signedWithKeyId: String
  ): ConnectionIO[Unit] = {
    sql"""
         |INSERT INTO revoked_keys (did_id, key_id)
         |VALUES ($signedWithDidId, $signedWithKeyId)
       """.stripMargin.update.run.void
  }

  def count(
      signedWithDidId: DidSuffix,
      signedWithKeyId: String
  ): ConnectionIO[Int] = {
    sql"""
         |SELECT COUNT(*)
         |FROM revoked_keys
         |WHERE did_id = $signedWithDidId and key_id = $signedWithKeyId
       """.stripMargin.query[Int].unique
  }
}
