package io.iohk.atala.prism.management.console.repositories.daos

import doobie.free.connection.ConnectionIO
import doobie.implicits._
import doobie.implicits.legacy.instant._
import io.circe.Json
import io.iohk.atala.prism.management.console.models.{
  Contact,
  CredentialIssuance,
  CredentialIssuanceContact,
  InstitutionGroup,
  ParticipantId
}

import java.time.Instant
import java.util.UUID

object CredentialIssuancesDAO {
  def createCredentialIssuance(issuance: CreateCredentialIssuance): ConnectionIO[CredentialIssuance.Id] = {
    val id = CredentialIssuance.Id(UUID.randomUUID())
    val createdOn = Instant.now()
    sql"""
         |INSERT INTO credential_issuances
         |  (credential_issuance_id, name, credential_type_id, status, created_by, created_at, ready_at)
         |VALUES ($id, ${issuance.name}, ${issuance.credentialTypeId}, ${issuance.status}, ${issuance.createdBy},
         |        $createdOn, $createdOn)
         |RETURNING credential_issuance_id
         |""".stripMargin.query[CredentialIssuance.Id].unique
  }

  def getCredentialIssuanceWithoutContacts(
      credentialIssuanceId: CredentialIssuance.Id,
      institutionId: ParticipantId
  ): ConnectionIO[CredentialIssuance] = {
    sql"""
         |SELECT credential_issuance_id, name, credential_type_id, status, created_at, ready_at
         |  FROM credential_issuances
         |  WHERE credential_issuance_id = $credentialIssuanceId
         |    AND created_by = $institutionId
         |""".stripMargin.query[CredentialIssuance].unique
  }

  def createContact(contact: CreateCredentialIssuanceContact): ConnectionIO[CredentialIssuance.ContactId] = {
    val id = CredentialIssuance.ContactId(UUID.randomUUID())
    sql"""
         |INSERT INTO credential_issuance_contacts
         |  (credential_issuance_contact_id, credential_issuance_id, contact_id, credential_data)
         |VALUES ($id, ${contact.credentialIssuanceId}, ${contact.contactId}, ${contact.credentialData})
         |RETURNING credential_issuance_contact_id
         |""".stripMargin.query[CredentialIssuance.ContactId].unique
  }

  def listContacts(credentialIssuanceId: CredentialIssuance.Id): ConnectionIO[List[CredentialIssuanceContact]] = {
    sql"""
         |SELECT credential_issuance_contact_id, contact_id, credential_data
         |  FROM credential_issuance_contacts
         |  WHERE credential_issuance_id = $credentialIssuanceId
         |  ORDER BY contact_id
         |""".stripMargin.query[CredentialIssuanceContact].to[List]
  }

  def createContactGroup(
      group: CreateCredentialIssuanceContactGroup
  ): ConnectionIO[CredentialIssuance.ContactGroupId] = {
    val id = CredentialIssuance.ContactGroupId(UUID.randomUUID())
    sql"""
         |INSERT INTO credential_issuance_groups (credential_issuance_group_id, credential_issuance_id, contact_group_id)
         |VALUES ($id, ${group.credentialIssuanceId}, ${group.contactGroupId})
         |RETURNING credential_issuance_group_id
         |""".stripMargin.query[CredentialIssuance.ContactGroupId].unique
  }

  def addContactToCredentialIssuance(
      contactId: CredentialIssuance.ContactId,
      credentialIssuanceId: CredentialIssuance.Id
  ): ConnectionIO[Unit] = {
    sql"""
         |INSERT INTO contacts_per_credential_issuance (credential_issuance_id, credential_issuance_contact_id)
         |VALUES ($credentialIssuanceId, $contactId)
         |""".stripMargin.update.run.map(_ => ())
  }

  def addContactToGroup(
      contactId: CredentialIssuance.ContactId,
      contactGroupId: CredentialIssuance.ContactGroupId
  ): ConnectionIO[Unit] = {
    sql"""
         |INSERT INTO contacts_per_credential_issuance_group (credential_issuance_group_id, credential_issuance_contact_id)
         |VALUES ($contactGroupId, $contactId)
         |""".stripMargin.update.run.map(_ => ())
  }

  case class CreateCredentialIssuance(
      name: String,
      createdBy: ParticipantId,
      credentialTypeId: Int,
      status: CredentialIssuance.Status
  )

  case class CreateCredentialIssuanceContact(
      credentialIssuanceId: CredentialIssuance.Id,
      contactId: Contact.Id,
      credentialData: Json
  )

  case class CreateCredentialIssuanceContactGroup(
      credentialIssuanceId: CredentialIssuance.Id,
      contactGroupId: InstitutionGroup.Id
  )
}
