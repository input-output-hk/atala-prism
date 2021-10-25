package io.iohk.atala.prism.management.console.repositories

import cats.Monad
import doobie._
import io.iohk.atala.prism.management.console.errors._
import io.iohk.atala.prism.management.console.models.{Contact, InstitutionGroup, ParticipantId}
import io.iohk.atala.prism.management.console.repositories.daos.{ContactsDAO, CredentialsDAO, InstitutionGroupsDAO}

object institutionHelper {
  // Make sure that all contacts belong to the given institution
  def checkContacts(
      institutionId: ParticipantId,
      contactIds: Set[Contact.Id]
  ): ConnectionIO[Option[ManagementConsoleError]] = {
    ContactsDAO.findContacts(institutionId, contactIds.toList).map { contacts =>
      val difference = contactIds.diff(contacts.map(_.contactId).toSet)
      if (difference.nonEmpty) {
        Some(ContactsInstitutionsDoNotMatch(difference.toList, institutionId))
      } else {
        None
      }
    }
  }

  // Make sure that all groups belong to the given institution
  def checkGroups(
      institutionId: ParticipantId,
      groupIds: Set[InstitutionGroup.Id]
  ): ConnectionIO[Option[ManagementConsoleError]] = {
    InstitutionGroupsDAO.findGroups(institutionId, groupIds.toList).map { groups =>
      val difference = groupIds.diff(groups.map(_.id).toSet)
      if (difference.nonEmpty) {
        Some(GroupsInstitutionDoNotMatch(difference.toList, institutionId))
      } else {
        None
      }
    }
  }

  def checkCredentialsAreEmpty(
      institutionId: ParticipantId,
      contactId: Contact.Id
  ): ConnectionIO[Option[ManagementConsoleError]] = {
    for {
      credentials <- CredentialsDAO.getBy(institutionId, contactId)
      result =
        if (credentials.isEmpty) {
          None
        } else {
          Some(ContactHasExistingCredentials(contactId))
        }
    } yield result
  }

  // Make sure that the group belongs to the given institution
  def checkGroupInstitution(
      institutionId: ParticipantId,
      group: InstitutionGroup
  ): ConnectionIO[Option[ManagementConsoleError]] = {
    if (group.institutionId == institutionId) {
      Monad[ConnectionIO].pure(None)
    } else {
      Monad[ConnectionIO].pure(
        Some(GroupInstitutionDoesNotMatch(group.institutionId, institutionId))
      )
    }
  }

  // Make sure that the group name is free
  def checkGroupNameIsFree(
      institutionId: ParticipantId,
      groupName: InstitutionGroup.Name
  ): ConnectionIO[Option[ManagementConsoleError]] = {
    InstitutionGroupsDAO.find(institutionId, groupName).map {
      case Some(_) => Some(GroupNameIsNotFree(groupName))
      case None => None
    }
  }
}
