package io.iohk.cvp.cmanager.repositories.daos

import doobie.free.connection.ConnectionIO
import doobie.implicits._
import io.iohk.cvp.cmanager.models.Issuer

object IssuersDAO {

  def insert(data: Issuer): ConnectionIO[Unit] = {
    sql"""
         |INSERT INTO issuers (issuer_id, did, name)
         |VALUES (${data.id}, ${data.did}, ${data.name})
       """.stripMargin.update.run.map(_ => ())
  }
}
