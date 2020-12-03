package io.iohk.atala.prism.node.repositories.daos

import doobie.free.connection.ConnectionIO
import doobie.implicits._
import io.iohk.atala.prism.crypto.SHA256Digest
import io.iohk.atala.prism.identity.DIDSuffix

object DIDDataDAO {
  def insert(didSuffix: DIDSuffix, lastOperation: SHA256Digest): ConnectionIO[Unit] = {
    sql"""
         |INSERT INTO did_data (did_suffix, last_operation)
         |VALUES ($didSuffix, $lastOperation)
       """.stripMargin.update.run.map(_ => ())
  }

  def findByDidSuffix(didSuffix: DIDSuffix): ConnectionIO[Option[DIDSuffix]] = {
    sql"""
         |SELECT 1 FROM did_data
         |WHERE did_suffix = $didSuffix
       """.stripMargin.query[Int].option.map(_.map(_ => didSuffix))
  }

  def getLastOperation(didSuffix: DIDSuffix): ConnectionIO[Option[SHA256Digest]] = {
    sql"""
         |SELECT last_operation FROM did_data
         |WHERE did_suffix = $didSuffix
       """.stripMargin.query[SHA256Digest].option
  }

  def all(): ConnectionIO[Seq[DIDSuffix]] = {
    sql"""SELECT did_suffix FROM did_data"""
      .query[DIDSuffix]
      .to[Seq]

  }
}
