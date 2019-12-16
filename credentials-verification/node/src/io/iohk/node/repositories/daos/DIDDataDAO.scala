package io.iohk.node.repositories.daos

import doobie.free.connection.ConnectionIO
import doobie.implicits._
import io.iohk.node.models.{DIDSuffix, SHA256Digest}

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

  def all(): ConnectionIO[Seq[DIDSuffix]] = {
    sql"""SELECT did_suffix FROM did_data"""
      .query[DIDSuffix]
      .to[Seq]

  }
}
