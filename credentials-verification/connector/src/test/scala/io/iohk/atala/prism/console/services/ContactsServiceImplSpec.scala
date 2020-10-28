package io.iohk.atala.prism.console.services

import java.time.LocalDate
import java.util.UUID

import io.circe
import io.circe.{Json, parser}
import io.iohk.atala.prism.console.grpc.ProtoCodecs.toContactProto
import io.iohk.atala.prism.console.DataPreparation.{createIssuer, createIssuerGroup, createContact}
import io.iohk.atala.prism.connector.model.TokenString
import io.iohk.atala.prism.connector.{RpcSpecBase, SignedRequestsAuthenticator}
import io.iohk.atala.prism.connector.repositories.{ParticipantsRepository, RequestNoncesRepository}
import io.iohk.atala.prism.console.models.{Contact, Institution, IssuerGroup}
import io.iohk.atala.prism.console.repositories.ContactsRepository
import io.iohk.atala.prism.grpc.GrpcAuthenticationHeaderParser
import io.iohk.atala.prism.models.ParticipantId
import io.iohk.atala.prism.protos.{console_api, console_models}
import org.mockito.MockitoSugar._
import org.scalatest.OptionValues._

import scala.concurrent.duration.DurationDouble

class ContactsServiceImplSpec extends RpcSpecBase {

  private implicit val executionContext = scala.concurrent.ExecutionContext.global
  private implicit val pc: PatienceConfig = PatienceConfig(20.seconds, 20.millis)
  private val usingApiAs = usingApiAsConstructor(new console_api.ConsoleServiceGrpc.ConsoleServiceBlockingStub(_, _))

  private lazy val participantsRepository = new ParticipantsRepository(database)
  private lazy val contactsRepository = new ContactsRepository(database)
  private lazy val requestNoncesRepository = new RequestNoncesRepository.PostgresImpl(database)(executionContext)
  private lazy val nodeMock = mock[io.iohk.atala.prism.protos.node_api.NodeServiceGrpc.NodeService]
  private lazy val authenticator = new SignedRequestsAuthenticator(
    participantsRepository,
    requestNoncesRepository,
    nodeMock,
    GrpcAuthenticationHeaderParser
  )

  override def services =
    Seq(
      console_api.ConsoleServiceGrpc
        .bindService(
          new ContactsServiceImpl(contactsRepository, authenticator),
          executionContext
        )
    )

  "createContact" should {
    "create a contact and assign it to a group" in {
      val issuerId = createIssuer("issuer name")
      val group = createIssuerGroup(issuerId, IssuerGroup.Name("group 1"))
      val externalId = Contact.ExternalId.random()

      usingApiAs(toParticipantId(issuerId)) { serviceStub =>
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

        val response = serviceStub.createContact(request).contact.value
        parser.parse(response.jsonData).toOption.value must be(json)
        response.externalId must be(request.externalId)

        // the new contact needs to exist
        val result = contactsRepository.getBy(issuerId, None, Some(group.name), 10).value.futureValue.toOption.value
        result.size must be(1)
        val storedContact = result.headOption.value
        toContactProto(storedContact).copy(jsonData = "") must be(response.copy(jsonData = ""))
        storedContact.data must be(json)
      }
    }

    "create a contact and assign it to no group" in {
      val issuerId = createIssuer("issuer name")
      val externalId = Contact.ExternalId.random()

      usingApiAs(toParticipantId(issuerId)) { serviceStub =>
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

        val response = serviceStub.createContact(request).contact.value
        val contactId = Contact.Id(UUID.fromString(response.contactId))
        parser.parse(response.jsonData).toOption.value must be(json)
        response.externalId must be(request.externalId)

        // the new contact needs to exist
        val result = contactsRepository.find(issuerId, contactId).value.futureValue.toOption.value
        val storedContact = result.value
        toContactProto(storedContact).copy(jsonData = "") must be(response.copy(jsonData = ""))
        storedContact.data must be(json)
      }
    }

    "fail to create a contact and assign it to a group that does not exists" in {
      val issuerId = createIssuer("issuer name")
      val externalId = Contact.ExternalId.random()

      usingApiAs(toParticipantId(issuerId)) { serviceStub =>
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

        intercept[Exception](
          serviceStub.createContact(request).contact.value
        )

        // the contact must not be added
        val result = contactsRepository.getBy(issuerId, None, None, 10).value.futureValue.toOption.value
        result must be(empty)
      }
    }

    "fail on attempt to create a contact with empty external id" in {
      val issuerId = createIssuer("issuer name")
      val group = createIssuerGroup(issuerId, IssuerGroup.Name("group 1"))

      usingApiAs(toParticipantId(issuerId)) { serviceStub =>
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

        intercept[Exception](
          serviceStub.createContact(request).contact.value
        )

        // the new contact should not exist
        val result = contactsRepository.getBy(issuerId, None, None, 10).value.futureValue.toOption.value
        result must be(empty)
      }
    }

    "fail on attempt to duplicate an external id" in {
      val issuerId = createIssuer("issuer name")
      val group = createIssuerGroup(issuerId, IssuerGroup.Name("group 1"))
      val externalId = Contact.ExternalId.random()

      usingApiAs(toParticipantId(issuerId)) { serviceStub =>
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
        val result = contactsRepository.getBy(issuerId, None, None, 10).value.futureValue.toOption.value
        result.size must be(1)

        val storedContact = result.head
        storedContact.data must be(json)
        storedContact.contactId.value.toString must be(initialResponse.contactId)
        storedContact.externalId must be(externalId)
      }
    }
  }

  private def cleanContactData(c: console_models.Contact): console_models.Contact = c.copy(jsonData = "")
  private def contactJsonData(c: console_models.Contact): Json = circe.parser.parse(c.jsonData).toOption.value

  "getContacts" should {
    "return the first contacts" in {
      val issuerId = createIssuer("Issuer X")
      val groupNameA = createIssuerGroup(issuerId, IssuerGroup.Name("Group A")).name
      val groupNameB = createIssuerGroup(issuerId, IssuerGroup.Name("Group B")).name
      val groupNameC = createIssuerGroup(issuerId, IssuerGroup.Name("Group C")).name
      val contactA = createContact(issuerId, "Alice", groupNameA)
      val contactB = createContact(issuerId, "Bob", groupNameB)
      createContact(issuerId, "Charles", groupNameC)
      createContact(issuerId, "Alice 2", groupNameA)

      usingApiAs(toParticipantId(issuerId)) { serviceStub =>
        val request = console_api.GetContactsRequest(
          limit = 2
        )

        val response = serviceStub.getContacts(request)
        val contactsReturned = response.contacts
        val contactsReturnedNoJsons = contactsReturned map cleanContactData
        val contactsReturnedJsons = contactsReturned map contactJsonData
        contactsReturnedNoJsons.toList must be(
          List(toContactProto(contactA), toContactProto(contactB)) map cleanContactData
        )
        contactsReturnedJsons.toList must be(List(contactA.data, contactB.data))
      }
    }

    "return the first contacts matching a group" in {
      val issuerId = createIssuer("Issuer X")
      val groupNameA = createIssuerGroup(issuerId, IssuerGroup.Name("Group A")).name
      val groupNameB = createIssuerGroup(issuerId, IssuerGroup.Name("Group B")).name
      val groupNameC = createIssuerGroup(issuerId, IssuerGroup.Name("Group C")).name
      val contactA = createContact(issuerId, "Alice", groupNameA)
      createContact(issuerId, "Bob", groupNameB)
      createContact(issuerId, "Charles", groupNameC)
      val contactA2 = createContact(issuerId, "Alice 2", groupNameA)

      usingApiAs(toParticipantId(issuerId)) { serviceStub =>
        val request = console_api.GetContactsRequest(
          limit = 2,
          groupName = groupNameA.value
        )

        val response = serviceStub.getContacts(request)
        val contactsReturned = response.contacts
        val contactsReturnedNoJsons = contactsReturned map cleanContactData
        val contactsReturnedJsons = contactsReturned map contactJsonData
        contactsReturnedNoJsons.toList must be(
          List(toContactProto(contactA), toContactProto(contactA2)) map cleanContactData
        )
        contactsReturnedJsons.toList must be(List(contactA.data, contactA2.data))
      }
    }

    "paginate by the last seen contact" in {
      val issuerId = createIssuer("Issuer X")
      val groupNameA = createIssuerGroup(issuerId, IssuerGroup.Name("Group A")).name
      val groupNameB = createIssuerGroup(issuerId, IssuerGroup.Name("Group B")).name
      val groupNameC = createIssuerGroup(issuerId, IssuerGroup.Name("Group C")).name
      createContact(issuerId, "Alice", groupNameA)
      val contactB = createContact(issuerId, "Bob", groupNameB)
      val contactC = createContact(issuerId, "Charles", groupNameC)
      createContact(issuerId, "Alice 2", groupNameA)

      usingApiAs(toParticipantId(issuerId)) { serviceStub =>
        val request = console_api.GetContactsRequest(
          limit = 1,
          lastSeenContactId = contactB.contactId.value.toString
        )

        val response = serviceStub.getContacts(request)
        val contactsReturned = response.contacts
        val contactsReturnedNoJsons = contactsReturned map cleanContactData
        val contactsReturnedJsons = contactsReturned map contactJsonData
        contactsReturnedNoJsons.toList must be(List(cleanContactData(toContactProto(contactC))))
        contactsReturnedJsons.toList must be(List(contactC.data))
      }
    }

    "paginate by the last seen contact matching by group" in {
      val issuerId = createIssuer("Issuer X")
      val groupNameA = createIssuerGroup(issuerId, IssuerGroup.Name("Group A")).name
      val groupNameB = createIssuerGroup(issuerId, IssuerGroup.Name("Group B")).name
      val groupNameC = createIssuerGroup(issuerId, IssuerGroup.Name("Group C")).name
      val contactA = createContact(issuerId, "Alice", groupNameA)
      createContact(issuerId, "Bob", groupNameB)
      createContact(issuerId, "Charles", groupNameC)
      val contactA2 = createContact(issuerId, "Alice 2", groupNameA)

      usingApiAs(toParticipantId(issuerId)) { serviceStub =>
        val request = console_api.GetContactsRequest(
          limit = 2,
          lastSeenContactId = contactA.contactId.value.toString,
          groupName = groupNameA.value
        )

        val response = serviceStub.getContacts(request)
        val contactsReturned = response.contacts
        val contactsReturnedNoJsons = contactsReturned map cleanContactData
        val contactsReturnedJsons = contactsReturned map contactJsonData
        contactsReturnedNoJsons.toList must be(List(cleanContactData(toContactProto(contactA2))))
        contactsReturnedJsons.toList must be(List(contactA2.data))
      }
    }
  }

  "getContact" should {
    "return the correct contact when present" in {
      val issuerId = createIssuer("Issuer X")
      val groupName = createIssuerGroup(issuerId, IssuerGroup.Name("Group A")).name
      val contact = createContact(issuerId, "Alice", groupName)
      createContact(issuerId, "Bob", groupName)

      usingApiAs(toParticipantId(issuerId)) { serviceStub =>
        val request = console_api.GetContactRequest(
          contactId = contact.contactId.value.toString
        )

        val response = serviceStub.getContact(request)
        cleanContactData(response.contact.value) must be(cleanContactData(toContactProto(contact)))
        contactJsonData(response.contact.value) must be(contact.data)
      }
    }

    "return no contact when the contact is missing (issuerId and contactId not correlated)" in {
      val issuerXId = createIssuer("Issuer X")
      val issuerYId = createIssuer("Issuer Y")
      val groupNameA = createIssuerGroup(issuerXId, IssuerGroup.Name("Group A")).name
      val groupNameB = createIssuerGroup(issuerYId, IssuerGroup.Name("Group B")).name
      val contact = createContact(issuerXId, "Alice", groupNameA)
      createContact(issuerYId, "Bob", groupNameB)

      usingApiAs(toParticipantId(issuerYId)) { serviceStub =>
        val request = console_api.GetContactRequest(
          contactId = contact.contactId.value.toString
        )

        val response = serviceStub.getContact(request)
        response.contact must be(empty)
      }
    }
  }

  "generateConnectionTokenForContact" should {
    "generate a token" in {
      val issuerName = "tokenizer"
      val groupName = IssuerGroup.Name("Grp 1")
      val contactName = "Contact 1"
      val issuerId = createIssuer(issuerName)
      createIssuerGroup(issuerId, groupName)
      val contact = createContact(issuerId, contactName, groupName)

      usingApiAs(toParticipantId(issuerId)) { serviceStub =>
        val request = console_api
          .GenerateConnectionTokenForContactRequest(
            contactId = contact.contactId.value.toString
          )

        val response = serviceStub.generateConnectionTokenForContact(request)
        val token = TokenString(response.token)

        // the new contact needs to exist
        val result = contactsRepository.find(issuerId, contact.contactId).value.futureValue.toOption.value
        val storedContact = result.value
        storedContact.contactId must be(contact.contactId)
        storedContact.data must be(contact.data)
        storedContact.createdAt must be(contact.createdAt)
        storedContact.connectionStatus must be(Contact.ConnectionStatus.ConnectionMissing)
        storedContact.connectionToken.value must be(token)
        storedContact.connectionId must be(contact.connectionId)
      }
    }
  }

  private def toParticipantId(issuer: Institution.Id): ParticipantId = {
    ParticipantId(issuer.value)
  }
}
