package io.iohk.atala.prism.management.console.repositories.daos

import java.time.Instant

import cats.data.NonEmptyList
import doobie.free.connection.ConnectionIO
import doobie.free.connection
import doobie.implicits.toSqlInterpolator
import doobie.implicits.legacy.instant._
import doobie.util.fragments.{in, whereAnd}
import io.iohk.atala.prism.management.console.models.{Contact, CreateContact, InstitutionGroup, ParticipantId}

object ContactsDAO {

  def createContact(data: CreateContact): ConnectionIO[Contact] = {
    val contactId = Contact.Id.random()
    val createdAt = Instant.now()
    sql"""
         |INSERT INTO contacts
         |  (contact_id, contact_data, created_at, created_by, external_id)
         |VALUES
         |  ($contactId, ${data.data}, $createdAt, ${data.createdBy}, ${data.externalId})
         |RETURNING contact_id, external_id, contact_data, created_at
         |""".stripMargin.query[Contact].unique
  }

  def findContact(participantId: ParticipantId, contactId: Contact.Id): doobie.ConnectionIO[Option[Contact]] = {
    sql"""
         |SELECT contact_id, external_id, contact_data, created_at
         |FROM contacts
         |WHERE contact_id = $contactId AND
         |      created_by = $participantId
         |""".stripMargin.query[Contact].option
  }

  def findContact(
      participantId: ParticipantId,
      externalId: Contact.ExternalId
  ): doobie.ConnectionIO[Option[Contact]] = {
    sql"""
         |SELECT contact_id, external_id, contact_data, created_at
         |FROM contacts
         |WHERE external_id = $externalId AND
         |      created_by = $participantId
         |""".stripMargin.query[Contact].option
  }

  def findContacts(institutionId: ParticipantId, contactIds: List[Contact.Id]): doobie.ConnectionIO[List[Contact]] = {
    NonEmptyList.fromList(contactIds) match {
      case Some(contactIdsNonEmpty) =>
        val fragment =
          fr"""
              |SELECT contact_id, external_id, contact_data, created_at
              |FROM contacts""".stripMargin ++
            whereAnd(fr"created_by = $institutionId", in(fr"contact_id", contactIdsNonEmpty))
        fragment.query[Contact].to[List]
      case None =>
        connection.pure(List.empty)
    }
  }

  def getBy(
      participantId: ParticipantId,
      scrollIdMaybe: Option[Contact.Id],
      limit: Int,
      groupName: Option[InstitutionGroup.Name]
  ): doobie.ConnectionIO[List[Contact]] = {

    val query = (scrollIdMaybe, groupName) match {
      case (Some(scrollId), Some(group)) =>
        sql"""
             |WITH CTE AS (
             |  SELECT COALESCE(max(created_at), to_timestamp(0)) AS last_seen_time
             |  FROM contacts
             |  WHERE contact_id = $scrollId
             |)
             |SELECT contact_id, external_id, contact_data, contacts.created_at
             |FROM CTE CROSS JOIN contacts
             |     JOIN contacts_per_group USING (contact_id)
             |     JOIN institution_groups g USING (group_id)
             |WHERE contacts.created_by = $participantId AND
             |      (contacts.created_at > last_seen_time OR (contacts.created_at = last_seen_time AND contact_id > $scrollId)) AND
             |      g.name = $group
             |ORDER BY contacts.created_at ASC, contact_id
             |LIMIT $limit
             |""".stripMargin
      case (Some(scrollId), None) =>
        sql"""
             |WITH CTE AS (
             |  SELECT COALESCE(max(created_at), to_timestamp(0)) AS last_seen_time
             |  FROM contacts
             |  WHERE contact_id = $scrollId
             |)
             |SELECT contact_id, external_id, contact_data, created_at
             |FROM CTE CROSS JOIN contacts
             |WHERE contacts.created_by = $participantId AND
             |      (created_at > last_seen_time OR (created_at = last_seen_time AND contact_id > $scrollId))
             |ORDER BY created_at ASC, contact_id
             |LIMIT $limit
             |""".stripMargin
      case (None, Some(group)) =>
        sql"""
             |SELECT contact_id, external_id, contact_data, contacts.created_at
             |FROM contacts
             |     JOIN contacts_per_group USING (contact_id)
             |     JOIN institution_groups g USING (group_id)
             |WHERE contacts.created_by = $participantId AND
             |      g.name = $group
             |ORDER BY contacts.created_at ASC, contact_id
             |LIMIT $limit
             |""".stripMargin
      case (None, None) =>
        sql"""
             |SELECT contact_id, external_id, contact_data, created_at
             |FROM contacts
             |WHERE created_by = $participantId
             |ORDER BY created_at ASC, contact_id
             |LIMIT $limit
             |""".stripMargin
    }
    query.query[Contact].to[List]
  }
}
