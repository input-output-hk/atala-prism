package io.iohk.node.repositories.daos

import doobie.free.connection.ConnectionIO
import doobie.implicits._
import io.iohk.node.models.DIDSuffix

object DIDDataDAO {
  def insert(didSuffix: DIDSuffix, lastOperation: Array[Byte]): ConnectionIO[Unit] = {
    sql"""
         |INSERT INTO did_data (did_suffix, last_operation)
         |VALUES ($didSuffix, $lastOperation)
       """.stripMargin.update.run.map(_ => ())
  }

  def findByDidSuffix(didSuffix: DIDSuffix): doobie.ConnectionIO[Option[DIDSuffix]] = {
    sql"""
         |SELECT 1 FROM did_data
         |WHERE did_suffix = $didSuffix
       """.stripMargin.query[Int].option.map(_.map(_ => didSuffix))
  }
}
