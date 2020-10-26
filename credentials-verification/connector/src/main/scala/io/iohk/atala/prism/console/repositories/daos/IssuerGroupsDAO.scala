package io.iohk.atala.prism.console.repositories.daos

import java.util.UUID

import cats.data.NonEmptyList
import cats.implicits.catsStdInstancesForList
import doobie.Update
import doobie.Fragments.{in, whereAnd}
import doobie.free.connection.{ConnectionIO, unit}
import doobie.implicits._
import doobie.implicits.legacy.instant._
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

  def find(groupId: IssuerGroup.Id): ConnectionIO[Option[IssuerGroup]] = {
    sql"""
         |SELECT group_id, name, issuer_id
         |FROM issuer_groups
         |WHERE group_id = $groupId
         |""".stripMargin.query[IssuerGroup].option
  }

  def find(issuer: Institution.Id, name: IssuerGroup.Name): ConnectionIO[Option[IssuerGroup]] = {
    sql"""
         |SELECT group_id, name, issuer_id
         |FROM issuer_groups
         |WHERE issuer_id = $issuer AND
         |      name = $name
       """.stripMargin.query[IssuerGroup].option
  }

  def listContacts(groupId: IssuerGroup.Id): ConnectionIO[List[Contact]] = {
    sql"""SELECT contact_id, external_id, contact_data, created_at, connection_status, connection_token, connection_id
         |FROM contacts_per_group
         |     JOIN contacts USING (contact_id)
         |WHERE group_id = $groupId
         |""".stripMargin.query[Contact].to[List]
  }

  def addContact(groupId: IssuerGroup.Id, contactId: Contact.Id): ConnectionIO[Unit] = {
    sql"""INSERT INTO contacts_per_group (group_id, contact_id, added_at)
         |VALUES ($groupId, $contactId, now())
         |""".stripMargin.update.run.map(_ => ())
  }

  def addContacts(groupId: IssuerGroup.Id, contactIds: List[Contact.Id]): ConnectionIO[Unit] = {
    val sql = """INSERT INTO contacts_per_group (group_id, contact_id, added_at)
                |VALUES (?, ?, now())
                |ON CONFLICT (group_id, contact_id) DO NOTHING
                |""".stripMargin
    Update[(IssuerGroup.Id, Contact.Id)](sql)
      .updateMany(contactIds.map(contactId => (groupId, contactId)))
      .map(_ => ())
  }

  def removeContacts(groupId: IssuerGroup.Id, contactIds: List[Contact.Id]): ConnectionIO[Unit] = {
    NonEmptyList.fromList(contactIds) match {
      case Some(contactIdsNonEmpty) =>
        val fragment = fr"DELETE FROM contacts_per_group" ++
          whereAnd(fr"group_id = $groupId", in(fr"contact_id", contactIdsNonEmpty))
        fragment.update.run.map(_ => ())
      case None =>
        unit
    }
  }
}
