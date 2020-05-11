package io.iohk.cvp.cmanager.repositories.daos

import doobie.free.connection.ConnectionIO
import doobie.implicits._
import io.iohk.cvp.cmanager.models.Issuer

object IssuersDAO {

  def insert(data: Issuer): ConnectionIO[Unit] = {
    sql"""
         |INSERT INTO issuers (issuer_id)
         |VALUES (${data.id})
       """.stripMargin.update.run.map(_ => ())
  }

  def findBy(id: Issuer.Id): ConnectionIO[Option[Issuer]] = {
    sql"""
         |SELECT issuer_id
         |FROM issuers
         |WHERE issuer_id = $id
         |""".stripMargin.query[Issuer].option
  }
}
