package io.iohk.atala.prism.vault.repositories.daos

import doobie.ConnectionIO
import doobie.implicits.toSqlInterpolator
import io.iohk.atala.prism.vault.model.Payload

object PayloadsDAO {
  def createPayload(id: Payload.Id): ConnectionIO[Payload] = {
    sql"""
         |INSERT INTO payloads (payload_id)
         |VALUES ($id)
         |RETURNING payload_id
         |""".stripMargin.query[Payload].unique
  }
}
