package io.iohk.atala.prism.management.console.services

import io.circe.{Json, parser}
import io.iohk.atala.prism.DIDGenerator
import io.iohk.atala.prism.auth.SignedRpcRequest
import io.iohk.atala.prism.crypto.EC
import io.iohk.atala.prism.management.console.DataPreparation._
import io.iohk.atala.prism.management.console.ManagementConsoleRpcSpecBase
import io.iohk.atala.prism.management.console.grpc.ProtoCodecs.toContactProto
import io.iohk.atala.prism.management.console.models.{Contact, CreateContact, InstitutionGroup, ParticipantId}
import io.iohk.atala.prism.protos.common_models.{HealthCheckRequest, HealthCheckResponse}
import io.iohk.atala.prism.protos.connector_api.ConnectionsStatusResponse
import io.iohk.atala.prism.protos.console_models.CredentialIssuanceStatus
import io.iohk.atala.prism.protos.{connector_models, console_api, console_models}
import org.mockito.ArgumentMatchersSugar.*
import org.mockito.IdiomaticMockito._
import org.scalatest.OptionValues._

import java.time.{Instant, LocalDate}
import java.util.UUID
import scala.concurrent.Future

class ConsoleServiceImplSpec extends ManagementConsoleRpcSpecBase with DIDGenerator {
  private val invitationMissing = connector_models.ContactConnection(
    connectionStatus = console_models.ContactConnectionStatus.INVITATION_MISSING
  )

  "health check" should {
    "respond" in {
      consoleService.healthCheck(HealthCheckRequest()).futureValue must be(HealthCheckResponse())
    }
  }

  "authentication" should {
    "support unpublished DID authentication" in {
      val keyPair = EC.generateKeyPair()
      val publicKey = keyPair.publicKey
      val did = generateDid(publicKey)
      val institutionId = createParticipant("Institution", did)
      val group = createInstitutionGroup(institutionId, InstitutionGroup.Name("group 1"))
      val externalId = Contact.ExternalId.random()
      val json = Json
        .obj(
          "universityAssignedId" -> Json.fromString("noneyet"),
          "fullName" -> Json.fromString("Alice Beakman"),
          "email" -> Json.fromString("alice@bkm.me"),
          "admissionDate" -> Json.fromString(LocalDate.now().toString)
        )
      val request = console_api
        .CreateContactRequest(
          groupName = group.name.value,
          jsonData = json.noSpaces,
          externalId = externalId.value
        )
      val rpcRequest = SignedRpcRequest.generate(keyPair, did, request)
      usingApiAs(rpcRequest) { blockingStub =>
        val response = blockingStub.createContact(request)
        response.contact.value.externalId must be(externalId.value)
      }
    }
  }

  "createContact" should {
    "create a contact and assign it to a group" in {
      val keyPair = EC.generateKeyPair()
      val publicKey = keyPair.publicKey
      val did = generateDid(publicKey)
      val institutionId = createParticipant("Institution", did)
      val group = createInstitutionGroup(institutionId, InstitutionGroup.Name("group 1"))
      val externalId = Contact.ExternalId.random()
      val json = Json
        .obj(
          "universityAssignedId" -> Json.fromString("noneyet"),
          "fullName" -> Json.fromString("Alice Beakman"),
          "email" -> Json.fromString("alice@bkm.me"),
          "admissionDate" -> Json.fromString(LocalDate.now().toString)
        )
      val request = console_api
        .CreateContactRequest(
          groupName = group.name.value,
          jsonData = json.noSpaces,
          externalId = externalId.value
        )
      val rpcRequest = SignedRpcRequest.generate(keyPair, did, request)

      usingApiAs(rpcRequest) { serviceStub =>
        val response = serviceStub.createContact(request).contact.value
        parser.parse(response.jsonData).toOption.value must be(json)
        response.externalId must be(request.externalId)

        // the new contact needs to exist
        val result =
          contactsRepository.getBy(institutionId, None, Some(group.name), 10).value.futureValue.toOption.value
        result.size must be(1)
        val storedContact = result.headOption.value
        toContactProto(storedContact, invitationMissing).copy(jsonData = "") must be(
          response.copy(jsonData = "")
        )
        storedContact.data must be(json)
      }
    }

    "create a contact and assign it to no group" in {
      val keyPair = EC.generateKeyPair()
      val publicKey = keyPair.publicKey
      val did = generateDid(publicKey)
      val institutionId = createParticipant("Institution", did)
      val externalId = Contact.ExternalId.random()
      val json = Json
        .obj(
          "universityAssignedId" -> Json.fromString("noneyet"),
          "fullName" -> Json.fromString("Alice Beakman"),
          "email" -> Json.fromString("alice@bkm.me"),
          "admissionDate" -> Json.fromString(LocalDate.now().toString)
        )

      val request = console_api
        .CreateContactRequest(
          jsonData = json.noSpaces,
          externalId = externalId.value
        )
      val rpcRequest = SignedRpcRequest.generate(keyPair, did, request)

      usingApiAs(rpcRequest) { serviceStub =>
        val response = serviceStub.createContact(request).contact.value
        val contactId = Contact.Id(UUID.fromString(response.contactId))
        parser.parse(response.jsonData).toOption.value must be(json)
        response.externalId must be(request.externalId)

        // the new contact needs to exist
        val result = contactsRepository.find(institutionId, contactId).value.futureValue.toOption.value
        val storedContact = result.value
        toContactProto(storedContact, invitationMissing).copy(jsonData = "") must be(
          response.copy(jsonData = "")
        )
        storedContact.data must be(json)
      }
    }

    "create a contact without JSON data" in {
      val keyPair = EC.generateKeyPair()
      val publicKey = keyPair.publicKey
      val did = generateDid(publicKey)
      val institutionId = createParticipant("Institution", did)
      val externalId = Contact.ExternalId.random()

      val request = console_api
        .CreateContactRequest(
          externalId = externalId.value
        )
      val rpcRequest = SignedRpcRequest.generate(keyPair, did, request)

      usingApiAs(rpcRequest) { serviceStub =>
        val response = serviceStub.createContact(request).contact.value
        val contactId = Contact.Id(UUID.fromString(response.contactId))
        parser.parse(response.jsonData).toOption.value must be(Json.obj())
        response.externalId must be(request.externalId)

        // the new contact needs to exist
        val result = contactsRepository.find(institutionId, contactId).value.futureValue.toOption.value
        val storedContact = result.value
        toContactProto(storedContact, invitationMissing) must be(response)
      }
    }

    "fail to create a contact and assign it to a group that does not exists" in {
      val keyPair = EC.generateKeyPair()
      val publicKey = keyPair.publicKey
      val did = generateDid(publicKey)
      val institutionId = createParticipant("Institution", did)
      val externalId = Contact.ExternalId.random()
      val json = Json
        .obj(
          "universityAssignedId" -> Json.fromString("noneyet"),
          "fullName" -> Json.fromString("Alice Beakman"),
          "email" -> Json.fromString("alice@bkm.me"),
          "admissionDate" -> Json.fromString(LocalDate.now().toString)
        )
      val request = console_api
        .CreateContactRequest(
          jsonData = json.noSpaces,
          externalId = externalId.value,
          groupName = "missing group"
        )
      val rpcRequest = SignedRpcRequest.generate(keyPair, did, request)

      usingApiAs(rpcRequest) { serviceStub =>
        intercept[Exception](
          serviceStub.createContact(request).contact.value
        )

        // the contact must not be added
        val result = contactsRepository.getBy(institutionId, None, None, 10).value.futureValue.toOption.value
        result must be(empty)
      }
    }

    "fail on attempt to create a contact with empty external id" in {
      val keyPair = EC.generateKeyPair()
      val publicKey = keyPair.publicKey
      val did = generateDid(publicKey)
      val institutionId = createParticipant("Institution", did)
      val group = createInstitutionGroup(institutionId, InstitutionGroup.Name("group 1"))
      val json = Json
        .obj(
          "universityAssignedId" -> Json.fromString("noneyet"),
          "fullName" -> Json.fromString("Alice Beakman"),
          "email" -> Json.fromString("alice@bkm.me"),
          "admissionDate" -> Json.fromString(LocalDate.now().toString)
        )
      val request = console_api
        .CreateContactRequest(
          groupName = group.name.value,
          jsonData = json.noSpaces
        )
      val rpcRequest = SignedRpcRequest.generate(keyPair, did, request)

      usingApiAs(rpcRequest) { serviceStub =>
        intercept[Exception](
          serviceStub.createContact(request).contact.value
        )

        // the new contact should not exist
        val result = contactsRepository.getBy(institutionId, None, None, 10).value.futureValue.toOption.value
        result must be(empty)
      }
    }

    "fail on attempt to duplicate an external id" in {
      val keyPair = EC.generateKeyPair()
      val publicKey = keyPair.publicKey
      val did = generateDid(publicKey)
      val institutionId = createParticipant("Institution", did)
      val group = createInstitutionGroup(institutionId, InstitutionGroup.Name("group 1"))
      val externalId = Contact.ExternalId.random()
      val json = Json
        .obj(
          "universityAssignedId" -> Json.fromString("noneyet"),
          "fullName" -> Json.fromString("Alice Beakman"),
          "email" -> Json.fromString("alice@bkm.me"),
          "admissionDate" -> Json.fromString(LocalDate.now().toString)
        )
      val request = console_api
        .CreateContactRequest(
          groupName = group.name.value,
          jsonData = json.noSpaces,
          externalId = externalId.value
        )
      val rpcRequest = SignedRpcRequest.generate(keyPair, did, request)

      usingApiAs(rpcRequest) { serviceStub =>
        val initialResponse = serviceStub.createContact(request).contact.value

        // We attempt to insert another contact with the same external id
        val secondJson = Json
          .obj(
            "universityAssignedId" -> Json.fromString("noneyet"),
            "fullName" -> Json.fromString("Second Contact"),
            "email" -> Json.fromString("second@test.me"),
            "admissionDate" -> Json.fromString(LocalDate.now().toString)
          )

        intercept[Exception](
          serviceStub.createContact(request.withJsonData(secondJson.noSpaces)).contact.value
        )

        // the contact needs to exist as originally inserted
        val result = contactsRepository.getBy(institutionId, None, None, 10).value.futureValue.toOption.value
        result.size must be(1)

        val storedContact = result.head
        storedContact.data must be(json)
        storedContact.contactId.value.toString must be(initialResponse.contactId)
        storedContact.externalId must be(externalId)
      }
    }
  }

  private def cleanContactData(c: console_models.Contact): console_models.Contact = c.copy(jsonData = "")
  private def contactJsonData(c: console_models.Contact): Json = parser.parse(c.jsonData).toOption.value

  "getContacts" should {
    "return the first contacts" in {
      val keyPair = EC.generateKeyPair()
      val publicKey = keyPair.publicKey
      val did = generateDid(publicKey)
      val institutionId = createParticipant("institutionx", did)
      val groupNameA = createInstitutionGroup(institutionId, InstitutionGroup.Name("Group A")).name
      val groupNameB = createInstitutionGroup(institutionId, InstitutionGroup.Name("Group B")).name
      val groupNameC = createInstitutionGroup(institutionId, InstitutionGroup.Name("Group C")).name
      val contactA = createContact(institutionId, "Alice", groupNameA)
      val contactB = createContact(institutionId, "Bob", groupNameB)
      createContact(institutionId, "Charles", groupNameC)
      createContact(institutionId, "Alice 2", groupNameA)
      val request = console_api.GetContactsRequest(
        limit = 2
      )
      val rpcRequest = SignedRpcRequest.generate(keyPair, did, request)

      usingApiAs(rpcRequest) { serviceStub =>
        connectorMock.getConnectionStatus(*).returns {
          Future.successful(
            ConnectionsStatusResponse(
              connections = List(invitationMissing, invitationMissing)
            )
          )
        }

        val response = serviceStub.getContacts(request)
        val contactsReturned = response.contacts
        val contactsReturnedNoJsons = contactsReturned map cleanContactData
        val contactsReturnedJsons = contactsReturned map contactJsonData
        contactsReturnedNoJsons.toList must be(
          List(
            toContactProto(contactA, invitationMissing),
            toContactProto(contactB, invitationMissing)
          ).map(cleanContactData)
        )
        contactsReturnedJsons.toList must be(List(contactA.data, contactB.data))
      }
    }

    "return the first contacts matching a group" in {
      val keyPair = EC.generateKeyPair()
      val publicKey = keyPair.publicKey
      val did = generateDid(publicKey)
      val institutionId = createParticipant("institutionx", did)
      val groupNameA = createInstitutionGroup(institutionId, InstitutionGroup.Name("Group A")).name
      val groupNameB = createInstitutionGroup(institutionId, InstitutionGroup.Name("Group B")).name
      val groupNameC = createInstitutionGroup(institutionId, InstitutionGroup.Name("Group C")).name
      val contactA = createContact(institutionId, "Alice", groupNameA)
      createContact(institutionId, "Bob", groupNameB)
      createContact(institutionId, "Charles", groupNameC)
      val contactA2 = createContact(institutionId, "Alice 2", groupNameA)
      val request = console_api.GetContactsRequest(
        limit = 2,
        groupName = groupNameA.value
      )
      val rpcRequest = SignedRpcRequest.generate(keyPair, did, request)

      usingApiAs(rpcRequest) { serviceStub =>
        connectorMock.getConnectionStatus(*).returns {
          Future.successful(
            ConnectionsStatusResponse(
              connections = List(invitationMissing, invitationMissing)
            )
          )
        }

        val response = serviceStub.getContacts(request)
        val contactsReturned = response.contacts
        val contactsReturnedNoJsons = contactsReturned map cleanContactData
        val contactsReturnedJsons = contactsReturned map contactJsonData
        contactsReturnedNoJsons.toList must be(
          List(
            toContactProto(contactA, invitationMissing),
            toContactProto(contactA2, invitationMissing)
          ).map(cleanContactData)
        )
        contactsReturnedJsons.toList must be(List(contactA.data, contactA2.data))
      }
    }

    "paginate by the last seen contact" in {
      val keyPair = EC.generateKeyPair()
      val publicKey = keyPair.publicKey
      val did = generateDid(publicKey)
      val institutionId = createParticipant("Institution X", did)
      val groupNameA = createInstitutionGroup(institutionId, InstitutionGroup.Name("Group A")).name
      val groupNameB = createInstitutionGroup(institutionId, InstitutionGroup.Name("Group B")).name
      val groupNameC = createInstitutionGroup(institutionId, InstitutionGroup.Name("Group C")).name
      createContact(institutionId, "Alice", groupNameA)
      val contactB = createContact(institutionId, "Bob", groupNameB)
      val contactC = createContact(institutionId, "Charles", groupNameC)
      createContact(institutionId, "Alice 2", groupNameA)
      val request = console_api.GetContactsRequest(
        limit = 1,
        lastSeenContactId = contactB.contactId.value.toString
      )
      val rpcRequest = SignedRpcRequest.generate(keyPair, did, request)

      usingApiAs(rpcRequest) { serviceStub =>
        connectorMock.getConnectionStatus(*).returns {
          Future.successful(
            ConnectionsStatusResponse(
              connections = List(invitationMissing)
            )
          )
        }

        val response = serviceStub.getContacts(request)
        val contactsReturned = response.contacts
        val contactsReturnedNoJsons = contactsReturned map cleanContactData
        val contactsReturnedJsons = contactsReturned map contactJsonData
        contactsReturnedNoJsons.toList must be(
          List(cleanContactData(toContactProto(contactC, invitationMissing)))
        )
        contactsReturnedJsons.toList must be(List(contactC.data))
      }
    }

    "paginate by the last seen contact matching by group" in {
      val keyPair = EC.generateKeyPair()
      val publicKey = keyPair.publicKey
      val did = generateDid(publicKey)
      val institutionId = createParticipant("Institution X", did)
      val groupNameA = createInstitutionGroup(institutionId, InstitutionGroup.Name("Group A")).name
      val groupNameB = createInstitutionGroup(institutionId, InstitutionGroup.Name("Group B")).name
      val groupNameC = createInstitutionGroup(institutionId, InstitutionGroup.Name("Group C")).name
      val contactA = createContact(institutionId, "Alice", groupNameA)
      createContact(institutionId, "Bob", groupNameB)
      createContact(institutionId, "Charles", groupNameC)
      val contactA2 = createContact(institutionId, "Alice 2", groupNameA)
      val request = console_api.GetContactsRequest(
        limit = 2,
        lastSeenContactId = contactA.contactId.value.toString,
        groupName = groupNameA.value
      )
      val rpcRequest = SignedRpcRequest.generate(keyPair, did, request)

      usingApiAs(rpcRequest) { serviceStub =>
        connectorMock.getConnectionStatus(*).returns {
          Future.successful(
            ConnectionsStatusResponse(
              connections = List(invitationMissing)
            )
          )
        }

        val response = serviceStub.getContacts(request)
        val contactsReturned = response.contacts
        val contactsReturnedNoJsons = contactsReturned map cleanContactData
        val contactsReturnedJsons = contactsReturned map contactJsonData
        contactsReturnedNoJsons.toList must be(
          List(cleanContactData(toContactProto(contactA2, invitationMissing)))
        )
        contactsReturnedJsons.toList must be(List(contactA2.data))
      }
    }
  }

  "getContact" should {
    "return the correct contact when present" in {
      val keyPair = EC.generateKeyPair()
      val publicKey = keyPair.publicKey
      val did = generateDid(publicKey)
      val institutionId = createParticipant("Institution X", did)
      val groupName = createInstitutionGroup(institutionId, InstitutionGroup.Name("Group A")).name
      val contact = createContact(institutionId, "Alice", groupName)
      createContact(institutionId, "Bob", groupName)
      val request = console_api.GetContactRequest(
        contactId = contact.contactId.value.toString
      )
      val rpcRequest = SignedRpcRequest.generate(keyPair, did, request)

      usingApiAs(rpcRequest) { serviceStub =>
        connectorMock.getConnectionStatus(*).returns {
          Future.successful(
            ConnectionsStatusResponse(
              connections = List(invitationMissing)
            )
          )
        }

        val response = serviceStub.getContact(request)
        cleanContactData(response.contact.value) must be(
          cleanContactData(toContactProto(contact, invitationMissing))
        )
        contactJsonData(response.contact.value) must be(contact.data)
      }
    }

    "return no contact when the contact is missing (institutionId and contactId not correlated)" in {
      val institutionXId = createParticipant("Institution X")
      val keyPairY = EC.generateKeyPair()
      val publicKeyY = keyPairY.publicKey
      val didY = generateDid(publicKeyY)
      val institutionYId = createParticipant("Institution Y", didY)
      val groupNameA = createInstitutionGroup(institutionXId, InstitutionGroup.Name("Group A")).name
      val groupNameB = createInstitutionGroup(institutionYId, InstitutionGroup.Name("Group B")).name
      val contact = createContact(institutionXId, "Alice", groupNameA)
      createContact(institutionYId, "Bob", groupNameB)
      val request = console_api.GetContactRequest(
        contactId = contact.contactId.value.toString
      )
      val rpcRequest = SignedRpcRequest.generate(keyPairY, didY, request)

      usingApiAs(rpcRequest) { serviceStub =>
        connectorMock.getConnectionStatus(*).returns {
          Future.successful(
            ConnectionsStatusResponse(
              connections = List()
            )
          )
        }

        val response = serviceStub.getContact(request)
        response.contact must be(empty)
      }
    }
  }

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
      usingApiAs(SignedRpcRequest.generate(keyPair, did, createRequest)) { serviceStub =>
        val creationTime = Instant.now
        val createResponse = serviceStub.createCredentialIssuance(createRequest)

        // Get the credential issuance just created
        val getRequest =
          console_api.GetCredentialIssuanceRequest(credentialIssuanceId = createResponse.credentialIssuanceId)
        usingApiAs(SignedRpcRequest.generate(keyPair, did, getRequest)) { serviceStub =>
          val credentialIssuance = serviceStub.getCredentialIssuance(getRequest)

          // Verify the obtained credential issuance matches the created one
          credentialIssuance.name mustBe createRequest.name
          credentialIssuance.credentialTypeId mustBe createRequest.credentialTypeId
          credentialIssuance.status mustBe CredentialIssuanceStatus.READY
          credentialIssuance.createdAt must (be >= creationTime.toEpochMilli and be <= Instant.now.toEpochMilli)
          // Verify contacts
          credentialIssuance.credentialIssuanceContacts.size mustBe contacts.size
          val issuanceContactsByContactId =
            credentialIssuance.credentialIssuanceContacts.map(contact => (contact.contactId, contact)).toMap
          for (contact <- contacts) {
            val issuanceContact = issuanceContactsByContactId(contact.contactId)
            issuanceContact.groupIds must contain theSameElementsAs contact.groupIds
            // Verify credential data
            val issuanceContactData = parser.parse(issuanceContact.credentialData).toOption.value
            val expectedContactData = parser.parse(contact.credentialData).toOption.value
            issuanceContactData mustBe expectedContactData
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
      usingApiAs(SignedRpcRequest.generate(keyPair, did, createRequest)) { serviceStub =>
        assertThrows[Exception] {
          serviceStub.createCredentialIssuance(createRequest)
        }
      }
    }

    "fail to get for a nonexistent ID" in {
      createParticipant("Institution", did)

      val getRequest =
        console_api.GetCredentialIssuanceRequest(credentialIssuanceId = UUID.randomUUID().toString)
      usingApiAs(SignedRpcRequest.generate(keyPair, did, getRequest)) { serviceStub =>
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

    contactsWithGroup ++ contactsWithoutGroup
  }

  private def createRandomCredentialIssuanceContact(
      institutionId: ParticipantId,
      group: Option[InstitutionGroup] = None
  ): console_models.CredentialIssuanceContact = {
    val contact = createRandomContact(institutionId, group.map(_.name))
    val contactId = contact.contactId.value.toString
    console_models.CredentialIssuanceContact(
      contactId = contactId,
      credentialData = s"""{"contactId": "$contactId"}""",
      groupIds = group.map(_.id.value.toString).toList
    )
  }

  private def createRandomContact(
      institutionId: ParticipantId,
      maybeGroupName: Option[InstitutionGroup.Name]
  ): Contact = {
    val contactData = CreateContact(institutionId, Contact.ExternalId.random(), Json.Null)
    contactsRepository.create(contactData, maybeGroupName).value.futureValue.toOption.value
  }

  "getStatistics" should {
    "work" in {
      val institutionName = "tokenizer"
      val groupName = InstitutionGroup.Name("Grp 1")
      val contactName = "Contact 1"
      val keyPair = EC.generateKeyPair()
      val publicKey = keyPair.publicKey
      val did = generateDid(publicKey)
      val institutionId = createParticipant(institutionName, did)
      createInstitutionGroup(institutionId, groupName)
      createContact(institutionId, contactName, groupName)
      val request = console_api.GetStatisticsRequest()
      val rpcRequest = SignedRpcRequest.generate(keyPair, did, request)

      usingApiAs(rpcRequest) { serviceStub =>
        val response = serviceStub.getStatistics(request)
        response.numberOfContacts must be(1)
        response.numberOfContactsConnected must be(0)
        response.numberOfContactsPendingConnection must be(0)
        response.numberOfGroups must be(1)
        response.numberOfCredentialsInDraft must be(0)
        response.numberOfCredentialsPublished must be(0)
        response.numberOfCredentialsReceived must be(0)
      }
    }
  }
}
