package io.iohk.atala.prism.management.console.repositories.daos

import java.time.Instant

import cats.data.NonEmptyList
import cats.implicits.catsStdInstancesForList
import doobie.Update
import doobie.Fragments.{in, whereAnd}
import doobie.free.connection.{ConnectionIO, unit}
import doobie.implicits._
import doobie.implicits.legacy.instant._
import io.iohk.atala.prism.management.console.models.{Contact, InstitutionGroup, ParticipantId}

object InstitutionGroupsDAO {

  def create(institutionId: ParticipantId, name: InstitutionGroup.Name): ConnectionIO[InstitutionGroup] = {
    val groupId = InstitutionGroup.Id.random()
    val now = Instant.now()
    sql"""
         |INSERT INTO institution_groups (group_id, institution_id, name, created_at)
         |VALUES ($groupId, $institutionId, $name, $now)
       """.stripMargin.update.run.map(_ => InstitutionGroup(groupId, name, institutionId, now))
  }

  def getBy(institutionId: ParticipantId): ConnectionIO[List[InstitutionGroup.WithContactCount]] = {
    sql"""
         |SELECT group_id, name, institution_id, created_at, (
         |  SELECT COUNT(*)
         |  FROM contacts_per_group
         |  WHERE group_id = g.group_id
         |) AS number_of_contacts
         |FROM institution_groups g
         |WHERE institution_id = $institutionId
         |ORDER BY name
       """.stripMargin.query[InstitutionGroup.WithContactCount].to[List]
  }

  def getBy(
      institutionId: ParticipantId,
      contactId: Contact.Id
  ): ConnectionIO[List[InstitutionGroup.WithContactCount]] = {
    sql"""
         |SELECT group_id, name, institution_id, created_at, (
         |  SELECT COUNT(*)
         |  FROM contacts_per_group
         |  WHERE group_id = g.group_id
         |) AS number_of_contacts
         |FROM institution_groups g join contacts_per_group cg USING (group_id)
         |WHERE institution_id = $institutionId AND contact_id = $contactId
         |ORDER BY name
       """.stripMargin.query[InstitutionGroup.WithContactCount].to[List]
  }

  def find(groupId: InstitutionGroup.Id): ConnectionIO[Option[InstitutionGroup]] = {
    sql"""
         |SELECT group_id, name, institution_id, created_at
         |FROM institution_groups
         |WHERE group_id = $groupId
         |""".stripMargin.query[InstitutionGroup].option
  }

  def find(institutionId: ParticipantId, name: InstitutionGroup.Name): ConnectionIO[Option[InstitutionGroup]] = {
    sql"""
         |SELECT group_id, name, institution_id, created_at
         |FROM institution_groups
         |WHERE institution_id = $institutionId AND
         |      name = $name
       """.stripMargin.query[InstitutionGroup].option
  }

  def listContacts(groupId: InstitutionGroup.Id): ConnectionIO[List[Contact]] = {
    sql"""SELECT contact_id, external_id, contact_data, created_at
         |FROM contacts_per_group
         |     JOIN contacts USING (contact_id)
         |WHERE group_id = $groupId
         |""".stripMargin.query[Contact].to[List]
  }

  def addContact(groupId: InstitutionGroup.Id, contactId: Contact.Id): ConnectionIO[Unit] = {
    sql"""INSERT INTO contacts_per_group (group_id, contact_id, added_at)
         |VALUES ($groupId, $contactId, now())
         |""".stripMargin.update.run.map(_ => ())
  }

  def addContacts(groupId: InstitutionGroup.Id, contactIds: List[Contact.Id]): ConnectionIO[Unit] = {
    val sql = """INSERT INTO contacts_per_group (group_id, contact_id, added_at)
                |VALUES (?, ?, now())
                |ON CONFLICT (group_id, contact_id) DO NOTHING
                |""".stripMargin
    Update[(InstitutionGroup.Id, Contact.Id)](sql)
      .updateMany(contactIds.map(contactId => (groupId, contactId)))
      .map(_ => ())
  }

  def removeContacts(groupId: InstitutionGroup.Id, contactIds: List[Contact.Id]): ConnectionIO[Unit] = {
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
