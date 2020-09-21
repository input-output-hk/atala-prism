package io.iohk.atala.prism.console.repositories.daos

import java.util.UUID

import doobie.free.connection.ConnectionIO
import doobie.implicits._
import io.iohk.atala.prism.console.models.{Contact, Institution, IssuerGroup}

object IssuerGroupsDAO {

  def create(issuerId: Institution.Id, name: IssuerGroup.Name): ConnectionIO[IssuerGroup] = {
    val groupId = IssuerGroup.Id(UUID.randomUUID())
    sql"""
         |INSERT INTO issuer_groups (group_id, issuer_id, name)
         |VALUES ($groupId, $issuerId, $name)
       """.stripMargin.update.run.map(_ => IssuerGroup(groupId, name, issuerId))
  }

  def getBy(issuer: Institution.Id): ConnectionIO[List[IssuerGroup.Name]] = {
    sql"""
         |SELECT name
         |FROM issuer_groups
         |WHERE issuer_id = $issuer
         |ORDER BY name
       """.stripMargin.query[IssuerGroup.Name].to[List]
  }

  def find(issuer: Institution.Id, name: IssuerGroup.Name): ConnectionIO[Option[IssuerGroup]] = {
    sql"""
         |SELECT group_id, name, issuer_id
         |FROM issuer_groups
         |WHERE issuer_id = $issuer AND
         |      name = $name
       """.stripMargin.query[IssuerGroup].option
  }

  def addContact(groupId: IssuerGroup.Id, contactId: Contact.Id): ConnectionIO[Unit] = {
    sql"""INSERT INTO contacts_per_group (group_id, contact_id, added_at)
         |VALUES ($groupId, $contactId, now())
         |""".stripMargin.update.run.map(_ => ())
  }
}
