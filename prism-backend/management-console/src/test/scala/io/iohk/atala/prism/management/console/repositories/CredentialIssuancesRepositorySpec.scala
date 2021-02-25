package io.iohk.atala.prism.management.console.repositories

import io.circe.Json
import io.iohk.atala.prism.AtalaWithPostgresSpec
import io.iohk.atala.prism.management.console.DataPreparation
import io.iohk.atala.prism.management.console.DataPreparation.{createInstitutionGroup, createParticipant}
import io.iohk.atala.prism.management.console.models.{Contact, InstitutionGroup}
import io.iohk.atala.prism.management.console.repositories.CredentialIssuancesRepository.{
  CreateCredentialIssuance,
  CreateCredentialIssuanceContact
}
import org.scalatest.OptionValues._

class CredentialIssuancesRepositorySpec extends AtalaWithPostgresSpec {
  private lazy val credentialIssuancesRepository = new CredentialIssuancesRepository(database)

  "create" should {
    "create a CredentialIssuance" in {
      val institutionId = createParticipant("The Institution")
      val aGroup = createInstitutionGroup(institutionId, InstitutionGroup.Name("A Group"))
      val contactsWithGroup: List[(Contact, Option[InstitutionGroup])] = List(
        DataPreparation.createContact(institutionId) -> None,
        DataPreparation.createContact(institutionId, groupName = Some(aGroup.name)) -> Some(aGroup)
      )

      val credentialTypeWithRequiredFields = DataPreparation.createCredentialType(institutionId, "name")

      val credentialIssuanceId = credentialIssuancesRepository
        .create(
          institutionId,
          CreateCredentialIssuance(
            name = "Credentials for everyone",
            credentialTypeId = credentialTypeWithRequiredFields.credentialType.id,
            contacts = contactsWithGroup.map { contactWithGroup =>
              val (contact: Contact, group: Option[InstitutionGroup]) = contactWithGroup
              CreateCredentialIssuanceContact(
                contactId = contact.contactId,
                credentialData = createCredentialData(contact),
                groupIds = group.map(_.id).toList
              )
            }
          )
        )
        .value
        .futureValue
        .toOption
        .value

      val credentialIssuance =
        credentialIssuancesRepository.get(credentialIssuanceId, institutionId).value.futureValue.toOption.value
      credentialIssuance.id mustBe credentialIssuanceId
      credentialIssuance.name mustBe "Credentials for everyone"
      credentialIssuance.credentialTypeId mustBe credentialTypeWithRequiredFields.credentialType.id
      credentialIssuance.contacts.size mustBe contactsWithGroup.size
      val issuanceContactsByContactId = credentialIssuance.contacts.map(contact => (contact.contactId, contact)).toMap
      for ((contact, group) <- contactsWithGroup) {
        val issuanceContact = issuanceContactsByContactId(contact.contactId)
        issuanceContact.credentialData mustBe createCredentialData(contact)
        issuanceContact.groupIds must contain theSameElementsAs group.map(_.id).toList
      }
    }
  }

  private def createCredentialData(contact: Contact): Json = {
    Json.obj("externalId" -> Json.fromString(contact.contactId.toString))
  }
}
