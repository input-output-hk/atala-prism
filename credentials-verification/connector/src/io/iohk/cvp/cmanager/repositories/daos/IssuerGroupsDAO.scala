package io.iohk.cvp.cmanager.repositories.daos

import java.util.UUID

import doobie.free.connection.ConnectionIO
import doobie.implicits._
import io.iohk.cvp.cmanager.models.Issuer

object IssuerGroupsDAO {

  def create(issuer: Issuer.Id, name: String): ConnectionIO[Unit] = {
    val groupId = UUID.randomUUID()
    sql"""
         |INSERT INTO issuer_groups (group_id, issuer_id, name)
         |VALUES (${groupId.toString}::UUID, $issuer, $name)
       """.stripMargin.update.run.map(_ => ())
  }

  def getBy(issuer: Issuer.Id): ConnectionIO[List[String]] = {
    sql"""
         |SELECT name
         |FROM issuer_groups
         |WHERE issuer_id = $issuer
         |ORDER BY name
       """.stripMargin.query[String].to[List]
  }
}
