package io.iohk.atala.prism.management.console.services

import io.circe.{Json, parser}
import io.grpc.StatusRuntimeException
import io.iohk.atala.prism.DIDGenerator
import io.iohk.atala.prism.auth.SignedRpcRequest
import io.iohk.atala.prism.crypto.EC
import io.iohk.atala.prism.management.console.DataPreparation._
import io.iohk.atala.prism.management.console.{DataPreparation, ManagementConsoleRpcSpecBase}
import io.iohk.atala.prism.management.console.grpc.ProtoCodecs.toContactProto
import io.iohk.atala.prism.management.console.models.{Contact, Helpers, InstitutionGroup, ParticipantId}
import io.iohk.atala.prism.protos.connector_api.ConnectionsStatusResponse
import io.iohk.atala.prism.protos.{connector_models, console_api, console_models}
import org.mockito.ArgumentMatchersSugar.*
import org.mockito.IdiomaticMockito._
import org.scalatest.OptionValues._

import java.time.LocalDate
import scala.concurrent.Future
import scala.util.Try

class ContactsServiceImplSpec extends ManagementConsoleRpcSpecBase with DIDGenerator {
  private val invitationMissing = connector_models.ContactConnection(
    connectionStatus = console_models.ContactConnectionStatus.INVITATION_MISSING
  )

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
      usingApiAsContacts(rpcRequest) { blockingStub =>
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

      usingApiAsContacts(rpcRequest) { serviceStub =>
        val response = serviceStub.createContact(request).contact.value
        parser.parse(response.jsonData).toOption.value must be(json)
        response.externalId must be(request.externalId)

        // the new contact needs to exist
        val result = contactsRepository
          .getBy(institutionId, Helpers.legacyQuery(None, Some(group.name), 10))
          .value
          .futureValue
          .toOption
          .value
        result.size must be(1)
        val storedContact = result.headOption.value.details
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

      usingApiAsContacts(rpcRequest) { serviceStub =>
        val response = serviceStub.createContact(request).contact.value
        val contactId = Contact.Id.unsafeFrom(response.contactId)
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

      usingApiAsContacts(rpcRequest) { serviceStub =>
        val response = serviceStub.createContact(request).contact.value
        val contactId = Contact.Id.unsafeFrom(response.contactId)
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

      usingApiAsContacts(rpcRequest) { serviceStub =>
        intercept[Exception](
          serviceStub.createContact(request).contact.value
        )

        // the contact must not be added
        val result = contactsRepository
          .getBy(institutionId, Helpers.legacyQuery(None, None, 10))
          .value
          .futureValue
          .toOption
          .value
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

      usingApiAsContacts(rpcRequest) { serviceStub =>
        intercept[Exception](
          serviceStub.createContact(request).contact.value
        )

        // the new contact should not exist
        val result = contactsRepository
          .getBy(institutionId, Helpers.legacyQuery(None, None, 10))
          .value
          .futureValue
          .toOption
          .value
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

      usingApiAsContacts(rpcRequest) { serviceStub =>
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
        val result = contactsRepository
          .getBy(institutionId, Helpers.legacyQuery(None, None, 10))
          .value
          .futureValue
          .toOption
          .value
        result.size must be(1)

        val storedContact = result.head.details
        storedContact.data must be(json)
        storedContact.contactId.toString must be(initialResponse.contactId)
        storedContact.externalId must be(externalId)
      }
    }
  }

  "createContacts" should {

    def runTest(requestBuilder: ParticipantId => console_api.CreateContactsRequest) = {
      val keyPair = EC.generateKeyPair()
      val publicKey = keyPair.publicKey
      val did = generateDid(publicKey)
      val institutionId = createParticipant("Institution", did)

      val request = requestBuilder(institutionId)
      val rpcRequest = SignedRpcRequest.generate(keyPair, did, request)

      lazy val response = usingApiAsContacts(rpcRequest) { serviceStub =>
        serviceStub.createContacts(request)
      }

      institutionId -> Try(response)
    }

    def testAvailableContacts(institutionId: ParticipantId, expected: Int) = {
      val result = contactsRepository
        .getBy(institutionId, Helpers.legacyQuery())
        .value
        .futureValue
        .toOption
        .value
      result.size must be(expected)
    }

    val json = Json.obj(
      "universityAssignedId" -> Json.fromString("noneyet"),
      "email" -> Json.fromString("alice@bkm.me"),
      "admissionDate" -> Json.fromString(LocalDate.now().toString)
    )

    "work when no groups are provided" in {
      val request = console_api
        .CreateContactsRequest()
        .withContacts(
          List(
            console_api.CreateContactsRequest
              .Contact()
              .withName("Alice")
              .withExternalId(Contact.ExternalId.random().value)
              .withJsonData(json.toString()),
            console_api.CreateContactsRequest
              .Contact()
              .withName("Bob")
              .withExternalId(Contact.ExternalId.random().value)
              .withJsonData(json.toString())
          )
        )
        .withGroups(List.empty)

      val (institutionId, responseT) = runTest(_ => request)
      responseT.isSuccess must be(true)
      testAvailableContacts(institutionId, 2)
    }

    "work when groups are provided assigning the contacts to the given groups" in {
      val groups = List("group 1", "group 2").map(InstitutionGroup.Name.apply)
      def request(institutionId: ParticipantId) = {
        val group1 = createInstitutionGroup(institutionId, groups(0))
        val group2 = createInstitutionGroup(institutionId, groups(1))
        console_api
          .CreateContactsRequest()
          .withContacts(
            List(
              console_api.CreateContactsRequest
                .Contact()
                .withName("Alice")
                .withExternalId(Contact.ExternalId.random().value)
                .withJsonData(json.toString()),
              console_api.CreateContactsRequest
                .Contact()
                .withName("Bob")
                .withExternalId(Contact.ExternalId.random().value)
                .withJsonData(json.toString())
            )
          )
          .withGroups(List(group1, group2).map(_.id.uuid.toString))
      }

      val (institutionId, responseT) = runTest(request)
      responseT.isSuccess must be(true)

      // the new contacts need to be assigned to the groups
      groups.foreach { group =>
        institutionGroupsRepository
          .listContacts(institutionId, group)
          .value
          .futureValue
          .toOption
          .value
          .size must be(2)
      }
    }

    "fail when there are unknown groups" in {
      def request(institutionId: ParticipantId) = {
        val group1 = createInstitutionGroup(institutionId, InstitutionGroup.Name("group 1"))
        val group2 = createInstitutionGroup(institutionId, InstitutionGroup.Name("group 2"))
        console_api
          .CreateContactsRequest()
          .withContacts(
            List(
              console_api.CreateContactsRequest
                .Contact()
                .withName("Alice")
                .withExternalId(Contact.ExternalId.random().value)
                .withJsonData(json.toString()),
              console_api.CreateContactsRequest
                .Contact()
                .withName("Bob")
                .withExternalId(Contact.ExternalId.random().value)
                .withJsonData(json.toString())
            )
          )
          .withGroups("wrong" :: List(group1, group2).map(_.id.uuid.toString))
      }

      val (institutionId, responseT) = runTest(request)
      responseT.isFailure must be(true)
      testAvailableContacts(institutionId, 0)
    }

    "fail when there are repeated groups" in {
      def request(institutionId: ParticipantId) = {
        val group1 = createInstitutionGroup(institutionId, InstitutionGroup.Name("group 1"))
        val group2 = createInstitutionGroup(institutionId, InstitutionGroup.Name("group 2"))
        console_api
          .CreateContactsRequest()
          .withContacts(
            List(
              console_api.CreateContactsRequest
                .Contact()
                .withName("Alice")
                .withExternalId(Contact.ExternalId.random().value)
                .withJsonData(json.toString()),
              console_api.CreateContactsRequest
                .Contact()
                .withName("Bob")
                .withExternalId(Contact.ExternalId.random().value)
                .withJsonData(json.toString())
            )
          )
          .withGroups(List(group1, group1, group2).map(_.id.uuid.toString))
      }

      val (institutionId, responseT) = runTest(request)
      responseT.isFailure must be(true)
      testAvailableContacts(institutionId, 0)
    }

    "fail when there are repeated contacts by externalId" in {
      val externalId = Contact.ExternalId.random().value
      val request = console_api
        .CreateContactsRequest()
        .withContacts(
          List(
            console_api.CreateContactsRequest
              .Contact()
              .withName("Alice")
              .withExternalId(externalId)
              .withJsonData(json.toString()),
            console_api.CreateContactsRequest
              .Contact()
              .withName("Bob")
              .withExternalId(externalId)
              .withJsonData(json.toString())
          )
        )
        .withGroups(List.empty)

      val (institutionId, responseT) = runTest(_ => request)
      responseT.isFailure must be(true)
      testAvailableContacts(institutionId, 0)
    }

    "fail when there are invalid contacts" in {
      val request = console_api
        .CreateContactsRequest()
        .withContacts(
          List(
            console_api.CreateContactsRequest
              .Contact()
              .withName("Alice")
              .withExternalId(Contact.ExternalId.random().value)
              .withJsonData("{{{{}"),
            console_api.CreateContactsRequest
              .Contact()
              .withName("Bob")
              .withExternalId(Contact.ExternalId.random().value)
              .withJsonData(json.toString())
          )
        )
        .withGroups(List.empty)

      val (institutionId, responseT) = runTest(_ => request)
      responseT.isFailure must be(true)
      testAvailableContacts(institutionId, 0)
    }

    "fail when there are no contacts" in {
      val request = console_api
        .CreateContactsRequest()
        .withContacts(List.empty)
        .withGroups(List.empty)

      val (institutionId, responseT) = runTest(_ => request)
      responseT.isFailure must be(true)
      testAvailableContacts(institutionId, 0)
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
      val contactA = createContact(institutionId, "Alice", Some(groupNameA))
      val contactB = createContact(institutionId, "Bob", Some(groupNameB))
      createContact(institutionId, "Charles", Some(groupNameC))
      createContact(institutionId, "Alice 2", Some(groupNameA))
      val request = console_api.GetContactsRequest(
        limit = 2
      )
      val rpcRequest = SignedRpcRequest.generate(keyPair, did, request)

      usingApiAsContacts(rpcRequest) { serviceStub =>
        connectorMock.getConnectionStatus(*).returns {
          Future.successful(
            ConnectionsStatusResponse(
              connections = List(invitationMissing, invitationMissing)
            )
          )
        }

        val response = serviceStub.getContacts(request)
        val contactsReturned = response.data.flatMap(_.contact)
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
      val contactA = createContact(institutionId, "Alice", Some(groupNameA))
      createContact(institutionId, "Bob", Some(groupNameB))
      createContact(institutionId, "Charles", Some(groupNameC))
      val contactA2 = createContact(institutionId, "Alice 2", Some(groupNameA))
      val request = console_api.GetContactsRequest(
        limit = 2,
        groupName = groupNameA.value
      )
      val rpcRequest = SignedRpcRequest.generate(keyPair, did, request)

      usingApiAsContacts(rpcRequest) { serviceStub =>
        connectorMock.getConnectionStatus(*).returns {
          Future.successful(
            ConnectionsStatusResponse(
              connections = List(invitationMissing, invitationMissing)
            )
          )
        }

        val response = serviceStub.getContacts(request)
        val contactsReturned = response.data.flatMap(_.contact)
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
      createContact(institutionId, "Alice", Some(groupNameA))
      val contactB = createContact(institutionId, "Bob", Some(groupNameB))
      val contactC = createContact(institutionId, "Charles", Some(groupNameC))
      createContact(institutionId, "Alice 2", Some(groupNameA))
      val request = console_api.GetContactsRequest(
        limit = 1,
        scrollId = contactB.contactId.toString
      )
      val rpcRequest = SignedRpcRequest.generate(keyPair, did, request)

      usingApiAsContacts(rpcRequest) { serviceStub =>
        connectorMock.getConnectionStatus(*).returns {
          Future.successful(
            ConnectionsStatusResponse(
              connections = List(invitationMissing)
            )
          )
        }

        val response = serviceStub.getContacts(request)
        val contactsReturned = response.data.flatMap(_.contact)
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
      val contactA = createContact(institutionId, "Alice", Some(groupNameA))
      createContact(institutionId, "Bob", Some(groupNameB))
      createContact(institutionId, "Charles", Some(groupNameC))
      val contactA2 = createContact(institutionId, "Alice 2", Some(groupNameA))
      val request = console_api.GetContactsRequest(
        limit = 2,
        scrollId = contactA.contactId.toString,
        groupName = groupNameA.value
      )
      val rpcRequest = SignedRpcRequest.generate(keyPair, did, request)

      usingApiAsContacts(rpcRequest) { serviceStub =>
        connectorMock.getConnectionStatus(*).returns {
          Future.successful(
            ConnectionsStatusResponse(
              connections = List(invitationMissing)
            )
          )
        }

        val response = serviceStub.getContacts(request)
        val contactsReturned = response.data.flatMap(_.contact)
        val contactsReturnedNoJsons = contactsReturned map cleanContactData
        val contactsReturnedJsons = contactsReturned map contactJsonData
        contactsReturnedNoJsons.toList must be(
          List(cleanContactData(toContactProto(contactA2, invitationMissing)))
        )
        contactsReturnedJsons.toList must be(List(contactA2.data))
      }
    }

    "return the credential counts" in {
      val keyPair = EC.generateKeyPair()
      val publicKey = keyPair.publicKey
      val did = generateDid(publicKey)
      val institutionId = createParticipant("institutionx", did)
      val contactA = createContact(institutionId, "Alice")
      DataPreparation.createGenericCredential(institutionId, contactA.contactId)
      DataPreparation.createGenericCredential(institutionId, contactA.contactId)
      DataPreparation.createReceivedCredential(contactA.contactId)
      val request = console_api.GetContactsRequest(
        limit = 2
      )
      val rpcRequest = SignedRpcRequest.generate(keyPair, did, request)

      usingApiAsContacts(rpcRequest) { serviceStub =>
        connectorMock.getConnectionStatus(*).returns {
          Future.successful(
            ConnectionsStatusResponse(
              connections = List(invitationMissing, invitationMissing)
            )
          )
        }

        val response = serviceStub.getContacts(request)
        val (created, received) = response.data
          .map(r => r.numberOfCredentialsCreated -> r.numberOfCredentialsReceived)
          .head

        created must be(2)
        received must be(1)
      }
    }

    def testCall[T](
        request: console_api.GetContactsRequest
    )(f: console_api.ContactsServiceGrpc.ContactsServiceBlockingStub => T) = {
      val keyPair = EC.generateKeyPair()
      val publicKey = keyPair.publicKey
      val did = generateDid(publicKey)
      createParticipant("institutionx", did)
      val rpcRequest = SignedRpcRequest.generate(keyPair, did, request)
      usingApiAsContacts(rpcRequest)(f)
    }

    List(-1, 101).foreach { limit =>
      s"fail when the limit is invalid: $limit" in {
        val request = console_api.GetContactsRequest(
          limit = limit
        )

        testCall(request) { serviceStub =>
          intercept[StatusRuntimeException] {
            serviceStub.getContacts(request)
          }
        }
      }
    }

    List(0, 1, 100).foreach { limit =>
      s"succeed when the limit is valid: $limit" in {
        val request = console_api.GetContactsRequest(
          limit = limit
        )

        testCall(request) { serviceStub =>
          serviceStub.getContacts(request)
        }
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
      val contact = createContact(institutionId, "Alice", Some(groupName))
      createContact(institutionId, "Bob", Some(groupName))
      val request = console_api.GetContactRequest(
        contactId = contact.contactId.toString
      )
      val rpcRequest = SignedRpcRequest.generate(keyPair, did, request)

      usingApiAsContacts(rpcRequest) { serviceStub =>
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
      val contact = createContact(institutionXId, "Alice", Some(groupNameA))
      createContact(institutionYId, "Bob", Some(groupNameB))
      val request = console_api.GetContactRequest(
        contactId = contact.contactId.toString
      )
      val rpcRequest = SignedRpcRequest.generate(keyPairY, didY, request)

      usingApiAsContacts(rpcRequest) { serviceStub =>
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
}
