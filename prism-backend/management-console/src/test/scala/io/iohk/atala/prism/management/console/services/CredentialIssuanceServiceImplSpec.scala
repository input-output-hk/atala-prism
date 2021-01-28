package io.iohk.atala.prism.management.console.services

import io.circe.{Json, parser}
import io.iohk.atala.prism.DIDGenerator
import io.iohk.atala.prism.auth.SignedRpcRequest
import io.iohk.atala.prism.crypto.EC
import io.iohk.atala.prism.management.console.DataPreparation._
import io.iohk.atala.prism.management.console.models.{InstitutionGroup, ParticipantId}
import io.iohk.atala.prism.management.console.{DataPreparation, ManagementConsoleRpcSpecBase}
import io.iohk.atala.prism.protos.console_models.CredentialIssuanceStatus
import io.iohk.atala.prism.protos.{console_api, console_models}
import org.mockito.IdiomaticMockito._
import org.scalatest.OptionValues._

import java.time.Instant
import java.util.UUID

class CredentialIssuanceServiceImplSpec extends ManagementConsoleRpcSpecBase with DIDGenerator {

  "createCredentialIssuance and getCredentialIssuance" should {
    val keyPair = EC.generateKeyPair()
    val did = generateDid(keyPair.publicKey)
    val otherKeyPair = EC.generateKeyPair()
    val otherDid = generateDid(otherKeyPair.publicKey)

    def createCredentialIssuanceRequest(
        contacts: List[console_models.CredentialIssuanceContact]
    ): console_api.CreateCredentialIssuanceRequest = {
      console_api.CreateCredentialIssuanceRequest(
        name = "2021 Class",
        credentialTypeId = 1,
        credentialIssuanceContacts = contacts
      )
    }

    "create and get a credential issuance" in {
      val institutionId = createParticipant("Institution", did)
      val contacts = createRandomCredentialIssuanceContacts(institutionId)

      // Create the credential issuance
      val createRequest = createCredentialIssuanceRequest(contacts)
      usingApiAsCredentialIssuance(SignedRpcRequest.generate(keyPair, did, createRequest)) { serviceStub =>
        val creationTime = Instant.now
        val createResponse = serviceStub.createCredentialIssuance(createRequest)

        // Verify the generated credentials
        val credentials =
          credentialsRepository
            .getBy(institutionId, contacts.size + 1, None)
            .value
            .futureValue
            .toOption
            .value
            .sortBy(_.subjectId.toString)
        credentials.size mustBe contacts.size
        contacts.zip(credentials).foreach {
          case (contact, credential) =>
            credential.subjectId.toString mustBe contact.contactId
            credential.credentialData mustBe asJson(contact.credentialData)
            credential.createdOn must (be >= creationTime and be <= Instant.now)
        }

        // Get the credential issuance just created and verify it matches
        val getRequest =
          console_api.GetCredentialIssuanceRequest(credentialIssuanceId = createResponse.credentialIssuanceId)
        usingApiAsCredentialIssuance(SignedRpcRequest.generate(keyPair, did, getRequest)) { serviceStub =>
          val credentialIssuance = serviceStub.getCredentialIssuance(getRequest)

          // Verify the obtained credential issuance matches the created one
          credentialIssuance.name mustBe createRequest.name
          credentialIssuance.credentialTypeId mustBe createRequest.credentialTypeId
          credentialIssuance.status mustBe CredentialIssuanceStatus.READY
          credentialIssuance.createdAt must (be >= creationTime.toEpochMilli and be <= Instant.now.toEpochMilli)
          // Verify contacts
          val issuanceContacts =
            credentialIssuance.credentialIssuanceContacts.sortBy(_.contactId)
          issuanceContacts.size mustBe contacts.size
          contacts.zip(issuanceContacts).foreach {
            case (contact, issuanceContact) =>
              issuanceContact.contactId mustBe contact.contactId
              issuanceContact.groupIds must contain theSameElementsAs contact.groupIds
              asJson(issuanceContact.credentialData) mustBe asJson(contact.credentialData)
          }
        }
      }
    }

    "fail to create for a contact outside the institution" in {
      val institutionId = createParticipant("Institution", did)
      val contacts = createRandomCredentialIssuanceContacts(institutionId)
      val otherInstitutionId = createParticipant("Other Institution", otherDid)
      val otherContacts = List(createRandomCredentialIssuanceContact(otherInstitutionId))

      val createRequest = createCredentialIssuanceRequest(contacts ++ otherContacts)
      usingApiAsCredentialIssuance(SignedRpcRequest.generate(keyPair, did, createRequest)) { serviceStub =>
        assertThrows[Exception] {
          serviceStub.createCredentialIssuance(createRequest)
        }
      }
    }

    "fail to get for a nonexistent ID" in {
      createParticipant("Institution", did)

      val getRequest =
        console_api.GetCredentialIssuanceRequest(credentialIssuanceId = UUID.randomUUID().toString)
      usingApiAsCredentialIssuance(SignedRpcRequest.generate(keyPair, did, getRequest)) { serviceStub =>
        assertThrows[Exception] {
          serviceStub.getCredentialIssuance(getRequest)
        }
      }
    }
  }

  private def createRandomCredentialIssuanceContacts(
      institutionId: ParticipantId
  ): List[console_models.CredentialIssuanceContact] = {
    val groups = List("Engineering", "Business").map { groupName =>
      institutionGroupsRepository
        .create(institutionId, InstitutionGroup.Name(groupName))
        .value
        .futureValue
        .toOption
        .value
    }
    val contactsWithGroup =
      groups.map { group =>
        createRandomCredentialIssuanceContact(institutionId, Some(group))
      }
    val contactsWithoutGroup = (1 to 2).map { _ =>
      createRandomCredentialIssuanceContact(institutionId)
    }

    (contactsWithGroup ++ contactsWithoutGroup).sortBy(_.contactId)
  }

  private def createRandomCredentialIssuanceContact(
      institutionId: ParticipantId,
      group: Option[InstitutionGroup] = None
  ): console_models.CredentialIssuanceContact = {
    val contact = DataPreparation.createContact(institutionId, groupName = group.map(_.name))
    val contactId = contact.contactId.toString
    console_models.CredentialIssuanceContact(
      contactId = contactId,
      credentialData = s"""{"contactId": "$contactId"}""",
      groupIds = group.map(_.id.toString).toList
    )
  }

  private def asJson(string: String): Json = parser.parse(string).toOption.value
}
