package io.iohk.atala.prism.management.console.repositories

import cats.effect.IO
import io.circe.Json
import io.iohk.atala.prism.AtalaWithPostgresSpec
import io.iohk.atala.prism.management.console.DataPreparation
import io.iohk.atala.prism.management.console.DataPreparation.{createInstitutionGroup, createParticipant}
import io.iohk.atala.prism.management.console.errors.CredentialDataValidationFailedForContacts
import io.iohk.atala.prism.management.console.models.{Contact, InstitutionGroup}
import io.iohk.atala.prism.management.console.repositories.CredentialIssuancesRepository.{
  CreateCredentialIssuance,
  CreateCredentialIssuanceContact
}
import io.iohk.atala.prism.utils.IOUtils._
import org.scalatest.OptionValues._
import tofu.logging.Logs

class CredentialIssuancesRepositorySpec extends AtalaWithPostgresSpec {
  val logs: Logs[IO, IO] = Logs.sync[IO, IO]
  private val credentialIssuancesRepository =
    CredentialIssuancesRepository.unsafe(database, logs)

  "create" should {
    "create a CredentialIssuance" in {
      val institutionId = createParticipant("The Institution")
      val aGroup =
        createInstitutionGroup(institutionId, InstitutionGroup.Name("A Group"))
      val contactsWithGroup: List[(Contact, Option[InstitutionGroup])] = List(
        DataPreparation.createContact(institutionId) -> None,
        DataPreparation.createContact(
          institutionId,
          groupName = Some(aGroup.name)
        ) -> Some(aGroup)
      )

      val credentialTypeWithRequiredFields =
        DataPreparation.createCredentialType(institutionId, "name")

      val credentialIssuanceId = credentialIssuancesRepository
        .create(
          institutionId,
          CreateCredentialIssuance(
            name = "Credentials for everyone",
            credentialTypeId = credentialTypeWithRequiredFields.credentialType.id,
            contacts = contactsWithGroup.map { contactWithGroup =>
              val (contact: Contact, group: Option[InstitutionGroup]) =
                contactWithGroup
              CreateCredentialIssuanceContact(
                contactId = contact.contactId,
                credentialData = credentialData,
                groupIds = group.map(_.id).toList
              )
            }
          )
        )
        .unsafeRunSync()
        .toOption
        .value

      val credentialIssuance =
        credentialIssuancesRepository
          .get(credentialIssuanceId, institutionId)
          .unsafeRunSync()
      credentialIssuance.id mustBe credentialIssuanceId
      credentialIssuance.name mustBe "Credentials for everyone"
      credentialIssuance.credentialTypeId mustBe credentialTypeWithRequiredFields.credentialType.id
      credentialIssuance.contacts.size mustBe contactsWithGroup.size
      val issuanceContactsByContactId = credentialIssuance.contacts
        .map(contact => (contact.contactId, contact))
        .toMap
      for ((contact, group) <- contactsWithGroup) {
        val issuanceContact = issuanceContactsByContactId(contact.contactId)
        issuanceContact.credentialData mustBe credentialData
        issuanceContact.groupIds must contain theSameElementsAs group
          .map(_.id)
          .toList
      }
    }

    "fail to create a new credential issuance when referenced credential type can't be rendered with any of " +
      "the specified credential data" in {
        val institutionId = createParticipant("The Institution")
        val contactsWithGroup: List[(Contact, Option[InstitutionGroup])] = List(
          DataPreparation.createContact(institutionId) -> None,
          DataPreparation.createContact(institutionId) -> None
        )

        val credentialTypeWithRequiredFields =
          DataPreparation.createCredentialType(institutionId, "name")

        val result = credentialIssuancesRepository
          .create(
            institutionId,
            CreateCredentialIssuance(
              name = "Credentials for everyone",
              credentialTypeId = credentialTypeWithRequiredFields.credentialType.id,
              contacts = contactsWithGroup.map { contactWithGroup =>
                val (contact: Contact, group: Option[InstitutionGroup]) =
                  contactWithGroup
                CreateCredentialIssuanceContact(
                  contactId = contact.contactId,
                  credentialData = Json.obj(),
                  groupIds = group.map(_.id).toList
                )
              }
            )
          )
          .unsafeRunSync()

        result mustBe a[Left[CredentialDataValidationFailedForContacts, _]]
      }

  }

  val credentialData =
    Json.obj(
      "title" -> Json.fromString("Some title"),
      "enrollmentDate" -> Json.fromString("01/10/2010"),
      "graduationDate" -> Json.fromString("01/07/2015")
    )
}
