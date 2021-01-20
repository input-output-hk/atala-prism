package io.iohk.atala.prism.management.console.repositories

import cats.effect.IO
import cats.implicits._
import doobie.free.connection.ConnectionIO
import doobie.implicits._
import doobie.util.transactor.Transactor
import io.circe.Json
import io.iohk.atala.prism.management.console.errors.ManagementConsoleError
import io.iohk.atala.prism.management.console.models.{Contact, CredentialIssuance, InstitutionGroup, ParticipantId}
import io.iohk.atala.prism.management.console.repositories.daos.CredentialIssuancesDAO
import io.iohk.atala.prism.management.console.repositories.daos.CredentialIssuancesDAO.CreateCredentialIssuanceContactGroup
import io.iohk.atala.prism.utils.FutureEither
import io.iohk.atala.prism.utils.FutureEither.FutureEitherOps
import io.scalaland.chimney.dsl._

import scala.concurrent.ExecutionContext

class CredentialIssuancesRepository(xa: Transactor[IO])(implicit ec: ExecutionContext) {
  import CredentialIssuancesRepository._

  def create(
      createCredentialIssuance: CreateCredentialIssuance
  ): FutureEither[ManagementConsoleError, CredentialIssuance.Id] = {
    def createContacts(
        credentialIssuanceId: CredentialIssuance.Id
    ): ConnectionIO[List[(CreateCredentialIssuanceContact, CredentialIssuance.ContactId)]] = {
      createCredentialIssuance.contacts.map { contact =>
        CredentialIssuancesDAO
          .createContact(
            contact
              .into[CredentialIssuancesDAO.CreateCredentialIssuanceContact]
              .withFieldConst(_.credentialIssuanceId, credentialIssuanceId)
              .transform
          )
          .map(contact -> _)
      }.sequence
    }

    def createGroups(
        credentialIssuanceId: CredentialIssuance.Id
    ): ConnectionIO[List[(InstitutionGroup.Id, CredentialIssuance.ContactGroupId)]] = {
      createCredentialIssuance.contacts
        .flatten(_.groupIds)
        .distinct
        .map { groupId =>
          CredentialIssuancesDAO
            .createContactGroup(
              CreateCredentialIssuanceContactGroup(
                credentialIssuanceId = credentialIssuanceId,
                contactGroupId = groupId
              )
            )
            .map(groupId -> _)
        }
        .sequence
    }

    def linkContactsToCredentialIssuance(
        credentialIssuanceId: CredentialIssuance.Id,
        contactsWithIds: List[(CreateCredentialIssuanceContact, CredentialIssuance.ContactId)],
        issuanceGroupIdByGroupId: Map[InstitutionGroup.Id, CredentialIssuance.ContactGroupId]
    ): ConnectionIO[List[Unit]] = {
      contactsWithIds
        .flatten(contactWithId => {
          val (contact, issuanceContactId) = contactWithId
          if (contact.groupIds.isEmpty) {
            // Add contact directly to the credential issuance as it does not belong to any group
            List(CredentialIssuancesDAO.addContactToCredentialIssuance(issuanceContactId, credentialIssuanceId))
          } else {
            // Associate the contact to the groups it belongs to, implicitly associating it with the credential
            // issuance
            contact.groupIds.map(groupId =>
              CredentialIssuancesDAO.addContactToGroup(issuanceContactId, issuanceGroupIdByGroupId(groupId))
            )
          }
        })
        .sequence
    }

    val query = for {
      // Create the credential issuance
      credentialIssuanceId <- CredentialIssuancesDAO.createCredentialIssuance(
        createCredentialIssuance
          .into[CredentialIssuancesDAO.CreateCredentialIssuance]
          .withFieldConst(_.status, CredentialIssuance.Status.Ready)
          .transform
      )
      // Create the contacts (not associated to the issuance yet)
      contactsWithIds <- createContacts(credentialIssuanceId)
      // Create the groups the credential issuance was created from
      groupIds <- createGroups(credentialIssuanceId)
      // Map the group IDs so we can add contacts to them
      issuanceGroupIdByGroupId = groupIds.toMap
      // Associate each contact with the groups it belongs to, or add it to the credential issuance directly otherwise
      _ <- linkContactsToCredentialIssuance(credentialIssuanceId, contactsWithIds, issuanceGroupIdByGroupId)
    } yield credentialIssuanceId

    query
      .transact(xa)
      .unsafeToFuture()
      .map(Right(_))
      .toFutureEither
  }

  def get(
      credentialIssuanceId: CredentialIssuance.Id,
      institutionId: ParticipantId
  ): FutureEither[ManagementConsoleError, CredentialIssuance] = {
    val query = for {
      // Get the issuance first, as contacts are queried later
      issuanceWithoutContacts <-
        CredentialIssuancesDAO.getCredentialIssuanceWithoutContacts(credentialIssuanceId, institutionId)
      // Get the contacts without contacts, as they are queried later
      contactsWithoutGroups <- CredentialIssuancesDAO.listContacts(credentialIssuanceId)
      // Determine which contacts belong to which group
      groupsPerContactList <- CredentialIssuancesDAO.listGroupsPerContact(credentialIssuanceId)
      groupsPerContact = groupsPerContactList.groupMap(_._1)(_._2).withDefaultValue(List())
      contacts = contactsWithoutGroups.map { contact =>
        contact.copy(groupIds = groupsPerContact(contact.id))
      }
    } yield issuanceWithoutContacts.copy(contacts = contacts)

    query
      .transact(xa)
      .unsafeToFuture()
      .map(Right(_))
      .toFutureEither
  }
}

object CredentialIssuancesRepository {
  case class CreateCredentialIssuance(
      name: String,
      createdBy: ParticipantId,
      credentialTypeId: Int,
      contacts: List[CreateCredentialIssuanceContact]
  )

  case class CreateCredentialIssuanceContact(
      contactId: Contact.Id,
      credentialData: Json,
      groupIds: List[InstitutionGroup.Id]
  )
}
