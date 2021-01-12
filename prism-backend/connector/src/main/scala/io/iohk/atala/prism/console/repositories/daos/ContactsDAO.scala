package io.iohk.atala.prism.console.repositories.daos

import java.time.Instant
import java.util.UUID

import cats.data.NonEmptyList
import doobie.free.connection.ConnectionIO
import doobie.free.connection
import doobie.implicits.toSqlInterpolator
import doobie.implicits.legacy.instant._
import doobie.util.fragments.{in, whereAnd}
import io.iohk.atala.prism.connector.model.{ConnectionId, ConnectionStatus, TokenString}
import io.iohk.atala.prism.console.models.{Contact, CreateContact, Institution, IssuerGroup}

object ContactsDAO {

  def createContact(data: CreateContact): ConnectionIO[Contact] = {
    val contactId = Contact.Id(UUID.randomUUID())
    val createdAt = Instant.now()
    val connectionStatus: ConnectionStatus = ConnectionStatus.InvitationMissing
    sql"""
         |INSERT INTO contacts
         |  (contact_id, contact_data, created_at, connection_status, created_by, external_id)
         |VALUES
         |  ($contactId, ${data.data}, $createdAt, $connectionStatus::CONTACT_CONNECTION_STATUS_TYPE,
         |   ${data.createdBy}, ${data.externalId})
         |RETURNING contact_id, external_id, contact_data, created_at, connection_status, connection_token, connection_id
         |""".stripMargin.query[Contact].unique
  }

  def findContact(institutionId: Institution.Id, contactId: Contact.Id): doobie.ConnectionIO[Option[Contact]] = {
    sql"""
           |SELECT contact_id, external_id, contact_data, created_at, connection_status, connection_token, connection_id
           |FROM contacts
           |WHERE contact_id = $contactId AND
           |      created_by = $institutionId
           |""".stripMargin.query[Contact].option
  }

  def findContact(issuerId: Institution.Id, externalId: Contact.ExternalId): doobie.ConnectionIO[Option[Contact]] = {
    sql"""
         |SELECT contact_id, external_id, contact_data, created_at, connection_status::TEXT::STUDENT_CONNECTION_STATUS_TYPE, connection_token, connection_id
         |FROM contacts
         |WHERE external_id = $externalId AND
         |      created_by = $issuerId
         |""".stripMargin.query[Contact].option
  }

  def findContacts(issuerId: Institution.Id, contactIds: List[Contact.Id]): doobie.ConnectionIO[List[Contact]] = {
    NonEmptyList.fromList(contactIds) match {
      case Some(contactIdsNonEmpty) =>
        fr"""
            |SELECT contact_id, external_id, contact_data, created_at, connection_status, connection_token, connection_id
            |FROM contacts""".stripMargin
        val fragment =
          fr"""
              |SELECT contact_id, external_id, contact_data, created_at, connection_status, connection_token, connection_id
              |FROM contacts""".stripMargin ++
            whereAnd(fr"created_by = $issuerId", in(fr"contact_id", contactIdsNonEmpty))
        fragment.query[Contact].to[List]
      case None =>
        connection.pure(List.empty)
    }
  }

  def getBy(
      institutionId: Institution.Id,
      lastContactSeen: Option[Contact.Id],
      limit: Int,
      groupName: Option[IssuerGroup.Name]
  ): doobie.ConnectionIO[List[Contact]] = {

    val query = (lastContactSeen, groupName) match {
      case (Some(lastSeen), Some(group)) =>
        sql"""
               |WITH CTE AS (
               |  SELECT COALESCE(max(created_at), to_timestamp(0)) AS last_seen_time
               |  FROM contacts
               |  WHERE contact_id = $lastSeen
               |)
               |SELECT contact_id, external_id, contact_data, contacts.created_at, connection_status, connection_token, connection_id
               |FROM CTE CROSS JOIN contacts
               |     JOIN contacts_per_group USING (contact_id)
               |     JOIN issuer_groups g USING (group_id)
               |WHERE contacts.created_by = $institutionId AND
               |      (contacts.created_at > last_seen_time OR (contacts.created_at = last_seen_time AND contact_id > $lastSeen)) AND
               |      g.name = $group
               |ORDER BY contacts.created_at ASC, contact_id
               |LIMIT $limit
               |""".stripMargin
      case (Some(lastSeen), None) =>
        sql"""
               |WITH CTE AS (
               |  SELECT COALESCE(max(created_at), to_timestamp(0)) AS last_seen_time
               |  FROM contacts
               |  WHERE contact_id = $lastSeen
               |)
               |SELECT contact_id, external_id, contact_data, created_at, connection_status, connection_token, connection_id
               |FROM CTE CROSS JOIN contacts
               |WHERE contacts.created_by = $institutionId AND
               |      (created_at > last_seen_time OR (created_at = last_seen_time AND contact_id > $lastSeen))
               |ORDER BY created_at ASC, contact_id
               |LIMIT $limit
               |""".stripMargin
      case (None, Some(group)) =>
        sql"""
               |SELECT contact_id, external_id, contact_data, contacts.created_at, connection_status, connection_token, connection_id
               |FROM contacts
               |     JOIN contacts_per_group USING (contact_id)
               |     JOIN issuer_groups g USING (group_id)
               |WHERE contacts.created_by = $institutionId AND
               |      g.name = $group
               |ORDER BY contacts.created_at ASC, contact_id
               |LIMIT $limit
               |""".stripMargin
      case (None, None) =>
        sql"""
               |SELECT contact_id, external_id, contact_data, created_at, connection_status, connection_token, connection_id
               |FROM contacts
               |WHERE created_by = $institutionId
               |ORDER BY created_at ASC, contact_id
               |LIMIT $limit
               |""".stripMargin
    }
    query.query[Contact].to[List]
  }

  // Called when a connection request is sent
  def setConnectionToken(
      institutionId: Institution.Id,
      contactId: Contact.Id,
      token: TokenString
  ): ConnectionIO[Unit] = {
    sql"""
         |UPDATE contacts
         |SET connection_token = $token,
         |    connection_status = ${ConnectionStatus.ConnectionMissing: ConnectionStatus}::CONTACT_CONNECTION_STATUS_TYPE
         |WHERE created_by = $institutionId AND contact_id = $contactId
         |""".stripMargin.update.run.map(_ => ())
  }

  def setConnectionAsAccepted(
      createdBy: Institution.Id,
      connectionToken: TokenString,
      connectionId: ConnectionId
  ): ConnectionIO[Unit] = {
    sql"""
         |UPDATE contacts
         |SET connection_id = $connectionId,
         |    connection_status = ${ConnectionStatus.ConnectionAccepted: ConnectionStatus}::CONTACT_CONNECTION_STATUS_TYPE
         |WHERE connection_token = $connectionToken AND
         |      created_by = $createdBy
         |""".stripMargin.update.run.map(_ => ())
  }
}
