package io.iohk.atala.prism.cmanager.repositories.daos

import java.util.UUID

import doobie.free.connection.ConnectionIO
import doobie.implicits._
import io.iohk.atala.prism.cmanager.models.{Issuer, IssuerGroup}

object IssuerGroupsDAO {

  def create(issuer: Issuer.Id, name: IssuerGroup.Name): ConnectionIO[IssuerGroup] = {
    val groupId = IssuerGroup.Id(UUID.randomUUID())
    sql"""
         |INSERT INTO issuer_groups (group_id, issuer_id, name)
         |VALUES ($groupId, $issuer, $name)
       """.stripMargin.update.run.map(_ => IssuerGroup(groupId, name, issuer))
  }

  def getBy(issuer: Issuer.Id): ConnectionIO[List[IssuerGroup.Name]] = {
    sql"""
         |SELECT name
         |FROM issuer_groups
         |WHERE issuer_id = $issuer
         |ORDER BY name
       """.stripMargin.query[IssuerGroup.Name].to[List]
  }

  def find(issuer: Issuer.Id, name: IssuerGroup.Name): ConnectionIO[Option[IssuerGroup]] = {
    sql"""
         |SELECT group_id, name, issuer_id
         |FROM issuer_groups
         |WHERE issuer_id = $issuer AND
         |      name = $name
       """.stripMargin.query[IssuerGroup].option
  }
}
