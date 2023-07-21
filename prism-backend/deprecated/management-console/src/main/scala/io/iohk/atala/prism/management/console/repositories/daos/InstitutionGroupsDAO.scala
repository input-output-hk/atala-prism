package io.iohk.atala.prism.management.console.repositories.daos

import cats.data.NonEmptyList
import cats.implicits.catsStdInstancesForList
import cats.syntax.functor._
import doobie.Fragments.{in, whereAnd}
import doobie._
import doobie.free.connection
import doobie.free.connection.{ConnectionIO, unit}
import doobie.implicits._
import doobie.implicits.legacy.instant._
import io.iohk.atala.prism.management.console.models.{Contact, InstitutionGroup, ParticipantId}
import io.iohk.atala.prism.management.console.repositories.daos.queries.FindGroupsQueryBuilder

import java.time.Instant

object InstitutionGroupsDAO {

  def create(
      institutionId: ParticipantId,
      name: InstitutionGroup.Name
  ): ConnectionIO[InstitutionGroup] = {
    val groupId = InstitutionGroup.Id.random()
    val now = Instant.now()
    sql"""
         |INSERT INTO institution_groups (group_id, institution_id, name, created_at)
         |VALUES ($groupId, $institutionId, $name, $now)
       """.stripMargin.update.run
      .as(InstitutionGroup(groupId, name, institutionId, now))
  }

  def update(
      groupId: InstitutionGroup.Id,
      newName: InstitutionGroup.Name
  ): ConnectionIO[Boolean] = {
    sql"""
         |UPDATE institution_groups
         |SET name = $newName
         |WHERE group_id = $groupId
       """.stripMargin.update.run.map(_ == 1)
  }

  def getBy(
      institutionId: ParticipantId
  ): ConnectionIO[List[InstitutionGroup.WithContactCount]] = {
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

  def getBy(
      institutionId: ParticipantId,
      query: InstitutionGroup.PaginatedQuery
  ): ConnectionIO[List[InstitutionGroup.WithContactCount]] = {
    FindGroupsQueryBuilder
      .build(institutionId, query)
      .query[InstitutionGroup.WithContactCount]
      .to[List]
  }

  def getTotalNumberOfRecords(
      institutionId: ParticipantId,
      query: InstitutionGroup.PaginatedQuery
  ): ConnectionIO[Int] = {
    FindGroupsQueryBuilder
      .buildTotalNumberOfRecordsQuery(institutionId, query)
      .query[Int]
      .unique
  }

  def find(
      groupId: InstitutionGroup.Id
  ): ConnectionIO[Option[InstitutionGroup]] = {
    sql"""
         |SELECT group_id, name, institution_id, created_at
         |FROM institution_groups
         |WHERE group_id = $groupId
         |""".stripMargin.query[InstitutionGroup].option
  }

  def find(
      institutionId: ParticipantId,
      name: InstitutionGroup.Name
  ): ConnectionIO[Option[InstitutionGroup]] = {
    sql"""
         |SELECT group_id, name, institution_id, created_at
         |FROM institution_groups
         |WHERE institution_id = $institutionId AND
         |      name = $name
       """.stripMargin.query[InstitutionGroup].option
  }

  def deleteGroup(
      institutionId: ParticipantId,
      groupId: InstitutionGroup.Id
  ): ConnectionIO[Boolean] = {
    sql"""
         |DELETE FROM institution_groups
         |WHERE institution_id = $institutionId AND
         |      group_id = $groupId
         | """.stripMargin.update.run.map(_ == 1)
  }

  def listContacts(
      groupId: InstitutionGroup.Id
  ): ConnectionIO[List[Contact]] = {
    sql"""SELECT contact_id, connection_token, external_id, contact_data, created_at, contacts.name
         |FROM contacts_per_group
         |     JOIN contacts USING (contact_id)
         |WHERE group_id = $groupId
         |""".stripMargin.query[Contact].to[List]
  }

  def addContact(
      groupId: InstitutionGroup.Id,
      contactId: Contact.Id
  ): ConnectionIO[Unit] = {
    sql"""INSERT INTO contacts_per_group (group_id, contact_id, added_at)
         |VALUES ($groupId, $contactId, ${Instant.now()})
         |""".stripMargin.update.run.void
  }

  def addContacts(
      groupIds: Set[InstitutionGroup.Id],
      contactIds: Set[Contact.Id]
  ): ConnectionIO[Unit] = {
    val addedAt = Instant.now()
    val data = for {
      groupId <- groupIds
      contactId <- contactIds
    } yield (groupId, contactId, addedAt)

    val sql = """INSERT INTO contacts_per_group (group_id, contact_id, added_at)
                |VALUES (?, ?, ?)
                |ON CONFLICT (group_id, contact_id) DO NOTHING
                |""".stripMargin
    Update[(InstitutionGroup.Id, Contact.Id, Instant)](sql)
      .updateMany(data.toList)
      .void
  }

  def copyContacts(
      originalGroupId: InstitutionGroup.Id,
      newGroupId: InstitutionGroup.Id
  ): ConnectionIO[Unit] = {
    sql"""INSERT INTO contacts_per_group (group_id, contact_id, added_at)
         |SELECT $newGroupId, contact_id, now()
         |FROM contacts_per_group
         |WHERE group_id = $originalGroupId
       """.stripMargin.update.run.void
  }

  def removeContacts(
      groupId: InstitutionGroup.Id,
      contactIds: List[Contact.Id]
  ): ConnectionIO[Unit] = {
    NonEmptyList.fromList(contactIds) match {
      case Some(contactIdsNonEmpty) =>
        val fragment = fr"DELETE FROM contacts_per_group" ++
          whereAnd(
            fr"group_id = $groupId",
            in(fr"contact_id", contactIdsNonEmpty)
          )
        fragment.update.run.void
      case None =>
        unit
    }
  }

  def removeAllGroupContacts(
      groupId: InstitutionGroup.Id
  ): ConnectionIO[Unit] = {
    sql"""DELETE FROM contacts_per_group
         |WHERE group_id = $groupId
         |""".stripMargin.update.run.void
  }

  def removeContact(contactId: Contact.Id): ConnectionIO[Unit] =
    sql"DELETE FROM contacts_per_group WHERE contact_id = $contactId".update.run.void

  def findGroups(
      institutionId: ParticipantId,
      groupIds: List[InstitutionGroup.Id]
  ): doobie.ConnectionIO[List[InstitutionGroup]] = {
    NonEmptyList.fromList(groupIds) match {
      case Some(groupIdsNonEmpty) =>
        val fragment = fr"""
                        |SELECT group_id, name, institution_id, created_at
                        |FROM institution_groups
       """.stripMargin ++
          whereAnd(
            fr"institution_id = $institutionId",
            in(fr"group_id", groupIdsNonEmpty)
          )
        fragment.query[InstitutionGroup].to[List]

      case None => connection.pure(List.empty)
    }
  }
}
