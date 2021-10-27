package io.iohk.atala.prism.management.console.repositories.daos

import cats.syntax.functor._
import doobie.free.connection.ConnectionIO
import doobie.implicits._
import doobie.implicits.legacy.instant._
import io.circe.Json
import io.iohk.atala.prism.management.console.models.{
  Contact,
  CredentialIssuance,
  CredentialTypeId,
  InstitutionGroup,
  ParticipantId
}

import java.time.Instant

object CredentialIssuancesDAO {
  def createCredentialIssuance(
      issuance: CreateCredentialIssuance,
      institutionId: ParticipantId
  ): ConnectionIO[CredentialIssuance.Id] = {
    val id = CredentialIssuance.Id.random()
    val createdOn = Instant.now()
    sql"""
         |INSERT INTO credential_issuances
         |  (credential_issuance_id, name, credential_type_id, created_by, created_at)
         |VALUES ($id, ${issuance.name}, ${issuance.credentialTypeId}, $institutionId, $createdOn)
         |RETURNING credential_issuance_id
         |""".stripMargin.query[CredentialIssuance.Id].unique
  }

  def getCredentialIssuanceWithoutContacts(
      credentialIssuanceId: CredentialIssuance.Id,
      institutionId: ParticipantId
  ): ConnectionIO[CredentialIssuanceWithoutContacts] = {
    sql"""
         |SELECT credential_issuance_id, name, credential_type_id, created_at
         |  FROM credential_issuances
         |  WHERE credential_issuance_id = $credentialIssuanceId
         |    AND created_by = $institutionId
         |""".stripMargin.query[CredentialIssuanceWithoutContacts].unique
  }

  def createContact(
      contact: CreateCredentialIssuanceContact
  ): ConnectionIO[CredentialIssuance.ContactId] = {
    val id = CredentialIssuance.ContactId.random()
    sql"""
         |INSERT INTO credential_issuance_contacts
         |  (credential_issuance_contact_id, credential_issuance_id, contact_id, credential_data)
         |VALUES ($id, ${contact.credentialIssuanceId}, ${contact.contactId}, ${contact.credentialData})
         |RETURNING credential_issuance_contact_id
         |""".stripMargin.query[CredentialIssuance.ContactId].unique
  }

  def listContactsWithoutGroups(
      credentialIssuanceId: CredentialIssuance.Id
  ): ConnectionIO[List[CredentialIssuanceContactWithoutGroups]] = {
    sql"""
         |SELECT credential_issuance_contact_id, contact_id, credential_data
         |  FROM credential_issuance_contacts
         |  WHERE credential_issuance_id = $credentialIssuanceId
         |  ORDER BY contact_id
         |""".stripMargin.query[CredentialIssuanceContactWithoutGroups].to[List]
  }

  def createContactGroup(
      group: CreateCredentialIssuanceContactGroup
  ): ConnectionIO[CredentialIssuance.ContactGroupId] = {
    val id = CredentialIssuance.ContactGroupId.random()
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
         |""".stripMargin.update.run.void
  }

  def addContactToGroup(
      contactId: CredentialIssuance.ContactId,
      contactGroupId: CredentialIssuance.ContactGroupId
  ): ConnectionIO[Unit] = {
    sql"""
         |INSERT INTO contacts_per_credential_issuance_group (credential_issuance_group_id, credential_issuance_contact_id)
         |VALUES ($contactGroupId, $contactId)
         |""".stripMargin.update.run.void
  }

  def listGroupsPerContact(
      credentialIssuanceId: CredentialIssuance.Id
  ): ConnectionIO[List[(CredentialIssuance.ContactId, InstitutionGroup.Id)]] = {
    sql"""
         |SELECT ci_contact_groups.credential_issuance_contact_id, ci_groups.contact_group_id
         |  FROM contacts_per_credential_issuance_group AS ci_contact_groups
         |  JOIN credential_issuance_groups AS ci_groups
         |    ON ci_groups.credential_issuance_group_id = ci_contact_groups.credential_issuance_group_id
         |  WHERE ci_groups.credential_issuance_id = $credentialIssuanceId
         |""".stripMargin
      .query[(CredentialIssuance.ContactId, InstitutionGroup.Id)]
      .to[List]
  }

  case class CreateCredentialIssuance(
      name: String,
      credentialTypeId: CredentialTypeId
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

  case class CredentialIssuanceWithoutContacts(
      id: CredentialIssuance.Id,
      name: String,
      credentialTypeId: CredentialTypeId,
      createdAt: Instant
  )

  case class CredentialIssuanceContactWithoutGroups(
      id: CredentialIssuance.ContactId,
      contactId: Contact.Id,
      credentialData: Json
  )
}
