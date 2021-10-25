package io.iohk.atala.prism.management.console.repositories.daos

import cats.data.NonEmptyList
import cats.implicits._
import doobie._
import doobie.free.connection
import doobie.implicits._
import doobie.implicits.legacy.instant._
import doobie.util.fragments._
import io.circe.Json
import io.iohk.atala.prism.management.console.models.{Contact, CreateContact, ParticipantId, UpdateContact}
import io.iohk.atala.prism.management.console.repositories.daos.queries.FindContactsQueryBuilder
import io.iohk.atala.prism.models.ConnectionToken

import java.time.Instant

object ContactsDAO {

  def createContact(
      participantId: ParticipantId,
      data: CreateContact,
      createdAt: Instant,
      connectionToken: ConnectionToken
  ): ConnectionIO[Contact] = {
    val contactId = Contact.Id.random()
    sql"""
         |INSERT INTO contacts
         |  (contact_id, connection_token, contact_data, created_at, created_by, external_id, name)
         |VALUES
         |  ($contactId, $connectionToken, ${data.data}, $createdAt, $participantId, ${data.externalId}, ${data.name})
         |RETURNING contact_id, connection_token, external_id, contact_data, created_at, name
         |""".stripMargin.query[Contact].unique
  }

  def createContacts(
      institutionId: ParticipantId,
      contacts: List[CreateContact.NoOwner],
      createdAt: Instant,
      connectionTokens: List[ConnectionToken]
  ): ConnectionIO[List[Contact.Id]] = {
    type CreateContactItem = (
        Contact.Id,
        ConnectionToken,
        Json,
        Instant,
        ParticipantId,
        Contact.ExternalId,
        String
    )
    val contactIds = List.tabulate(contacts.size)(_ => Contact.Id.random())
    val data = contacts
      .zip(contactIds)
      .zip(connectionTokens)
      .map { case ((item, id), connectionToken) =>
        (
          id,
          connectionToken,
          item.data,
          createdAt,
          institutionId,
          item.externalId,
          item.name
        )
      }

    val statement =
      """
                    |INSERT INTO contacts (contact_id, connection_token, contact_data, created_at, created_by, external_id, name)
                    |VALUES (?, ?, ?, ?, ?, ?, ?)
                    |""".stripMargin
    Update[CreateContactItem](statement)
      .updateMany(data)
      .flatTap { affectedRows =>
        FC.raiseError(
          new RuntimeException(
            s"Unknown error while inserting ${contacts.size} contacts"
          )
        ).whenA(contacts.size != affectedRows)
      }
      .as(contactIds)
  }

  def updateContact(
      institutionId: ParticipantId,
      data: UpdateContact
  ): ConnectionIO[Unit] = {
    sql"""
         |UPDATE contacts
         |SET external_id = ${data.newExternalId},
         |    name = ${data.newName},
         |    contact_data = ${data.newData}
         |WHERE contact_id = ${data.id} AND
         |      created_by = $institutionId
         |""".stripMargin.update.run.flatTap { affectedRows =>
      FC.raiseError(
        new RuntimeException(
          s"Unable to update contact, it is likely that it doesn't exist"
        )
      ).whenA(1 != affectedRows)
    }.void
  }

  def findContact(
      participantId: ParticipantId,
      contactId: Contact.Id
  ): doobie.ConnectionIO[Option[Contact]] = {
    sql"""
         |SELECT contact_id, connection_token, external_id, contact_data, created_at, name
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
         |SELECT contact_id, connection_token, external_id, contact_data, created_at, name
         |FROM contacts
         |WHERE external_id = $externalId AND
         |      created_by = $participantId
         |""".stripMargin.query[Contact].option
  }

  def findByToken(
      participantId: ParticipantId,
      connectionToken: ConnectionToken
  ): doobie.ConnectionIO[Option[Contact]] = {
    sql"""
         |SELECT contact_id, connection_token, external_id, contact_data, created_at, name
         |FROM contacts
         |WHERE connection_token = $connectionToken AND
         |      created_by = $participantId
         |""".stripMargin.query[Contact].option
  }

  def findContacts(
      institutionId: ParticipantId,
      contactIds: List[Contact.Id]
  ): doobie.ConnectionIO[List[Contact]] = {
    NonEmptyList.fromList(contactIds) match {
      case Some(contactIdsNonEmpty) =>
        val fragment =
          fr"""
              |SELECT contact_id, connection_token, external_id, contact_data, created_at, name
              |FROM contacts""".stripMargin ++
            whereAnd(
              fr"created_by = $institutionId",
              in(fr"contact_id", contactIdsNonEmpty)
            )
        fragment.query[Contact].to[List]
      case None =>
        connection.pure(List.empty)
    }
  }

  def getBy(
      participantId: ParticipantId,
      constraints: Contact.PaginatedQuery,
      ignoreFilterLimit: Boolean = false
  ): doobie.ConnectionIO[List[Contact.WithCredentialCounts]] = {
    FindContactsQueryBuilder
      .build(participantId, constraints, ignoreFilterLimit)
      .query[Contact.WithCredentialCounts]
      .to[List]
  }

  def delete(
      participantId: ParticipantId,
      contactId: Contact.Id
  ): doobie.ConnectionIO[Boolean] = {
    sql"""
         |DELETE FROM contacts
         |WHERE contact_id = $contactId AND
         |      created_by = $participantId
         |""".stripMargin.update.run.map(_ == 1)
  }
}
