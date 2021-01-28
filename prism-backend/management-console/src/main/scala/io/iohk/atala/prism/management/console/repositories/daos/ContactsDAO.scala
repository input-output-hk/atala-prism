package io.iohk.atala.prism.management.console.repositories.daos

import cats.data.NonEmptyList
import doobie.free.connection
import doobie.free.connection.ConnectionIO
import doobie.implicits._
import doobie.implicits.legacy.instant._
import doobie.util.fragments._
import io.iohk.atala.prism.management.console.models.{Contact, CreateContact, ParticipantId}
import io.iohk.atala.prism.management.console.repositories.daos.queries.FindContactsQueryBuilder

import java.time.Instant

object ContactsDAO {

  def createContact(data: CreateContact, createdAt: Instant): ConnectionIO[Contact] = {
    val contactId = Contact.Id.random()
    sql"""
         |INSERT INTO contacts
         |  (contact_id, contact_data, created_at, created_by, external_id, name)
         |VALUES
         |  ($contactId, ${data.data}, $createdAt, ${data.createdBy}, ${data.externalId}, ${data.name})
         |RETURNING contact_id, external_id, contact_data, created_at, name
         |""".stripMargin.query[Contact].unique
  }

  def findContact(participantId: ParticipantId, contactId: Contact.Id): doobie.ConnectionIO[Option[Contact]] = {
    sql"""
         |SELECT contact_id, external_id, contact_data, created_at, name
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
         |SELECT contact_id, external_id, contact_data, created_at, name
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
              |SELECT contact_id, external_id, contact_data, created_at, name
              |FROM contacts""".stripMargin ++
            whereAnd(fr"created_by = $institutionId", in(fr"contact_id", contactIdsNonEmpty))
        fragment.query[Contact].to[List]
      case None =>
        connection.pure(List.empty)
    }
  }

  def getBy(participantId: ParticipantId, constraints: Contact.PaginatedQuery): doobie.ConnectionIO[List[Contact]] = {
    FindContactsQueryBuilder
      .build(participantId, constraints)
      .query[Contact]
      .to[List]
  }
}
