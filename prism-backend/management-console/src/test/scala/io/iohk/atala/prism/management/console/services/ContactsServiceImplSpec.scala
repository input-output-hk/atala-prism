package io.iohk.atala.prism.management.console.services

import cats.data.ReaderT
import cats.effect.IO
import cats.effect.unsafe.implicits.global
import io.circe.{Json, parser}
import io.grpc.StatusRuntimeException
import io.iohk.atala.prism.DIDUtil
import io.iohk.atala.prism.auth.SignedRpcRequest
import io.iohk.atala.prism.connector.AtalaOperationId
import io.iohk.atala.prism.crypto.EC.{INSTANCE => EC}
import io.iohk.atala.prism.logging.TraceId
import io.iohk.atala.prism.management.console.DataPreparation._
import io.iohk.atala.prism.management.console.grpc.ProtoCodecs.toContactProto
import io.iohk.atala.prism.management.console.models.Contact.ExternalId
import io.iohk.atala.prism.management.console.models.{Contact, Helpers, InstitutionGroup, ParticipantId}
import io.iohk.atala.prism.management.console.{DataPreparation, ManagementConsoleRpcSpecBase, ManagementConsoleTestUtil}
import io.iohk.atala.prism.models.ConnectionToken
import io.iohk.atala.prism.protos.console_api.DeleteContactResponse
import io.iohk.atala.prism.protos.console_models.ContactConnectionStatus
import io.iohk.atala.prism.protos.{connector_models, console_api, console_models}
import org.mockito.ArgumentMatchersSugar.*
import org.mockito.IdiomaticMockito._
import org.scalatest.OptionValues._

import java.time.LocalDate
import java.util.UUID
import scala.util.Try

class ContactsServiceImplSpec extends ManagementConsoleRpcSpecBase with DIDUtil with ManagementConsoleTestUtil {
  private def connectionMissing(connectionToken: Option[String] = None) =
    connector_models.ContactConnection(
      connectionStatus = console_models.ContactConnectionStatus.STATUS_CONNECTION_MISSING,
      connectionToken = connectionToken.getOrElse("")
    )

  "authentication" should {
    "support unpublished DID authentication" in {
      val keyPair = EC.generateKeyPair()
      val publicKey = keyPair.getPublicKey
      val did = generateDid(publicKey)
      val institutionId = createParticipant("Institution", did)
      val group =
        createInstitutionGroup(institutionId, InstitutionGroup.Name("group 1"))
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
          externalId = externalId.value,
          generateConnectionTokensRequestMetadata = Some(connectorRequestMetadataProto)
        )
      connectorMock.generateConnectionTokens(*, *).returns {
        ReaderT.liftF(
          IO.pure(
            List(ConnectionToken(UUID.randomUUID.toString))
          )
        )
      }
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
      val publicKey = keyPair.getPublicKey
      val did = generateDid(publicKey)
      val institutionId = createParticipant("Institution", did)
      val group =
        createInstitutionGroup(institutionId, InstitutionGroup.Name("group 1"))
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
          externalId = externalId.value,
          generateConnectionTokensRequestMetadata = Some(connectorRequestMetadataProto)
        )
      val rpcRequest = SignedRpcRequest.generate(keyPair, did, request)
      connectorMock.generateConnectionTokens(*, *).returns {
        ReaderT.liftF(
          IO.pure(
            List(ConnectionToken(UUID.randomUUID.toString))
          )
        )
      }

      usingApiAsContacts(rpcRequest) { serviceStub =>
        val response = serviceStub.createContact(request).contact.value
        parser.parse(response.jsonData).toOption.value must be(json)
        response.externalId must be(request.externalId)

        // the new contact needs to exist
        val result =
          contactsRepository
            .getBy(
              institutionId,
              Helpers.legacyQuery(None, Some(group.name), 10)
            )
            .run(TraceId.generateYOLO)
            .unsafeRunSync()
        result.size must be(1)
        val storedContact = result.headOption.value.details
        toContactProto(storedContact, connectionMissing())
          .copy(jsonData = "") must be(
          response.copy(jsonData = "")
        )
        storedContact.data must be(json)
      }
    }

    "create a contact and assign it to no group" in {
      val keyPair = EC.generateKeyPair()
      val publicKey = keyPair.getPublicKey
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
          generateConnectionTokensRequestMetadata = Some(connectorRequestMetadataProto)
        )
      val rpcRequest = SignedRpcRequest.generate(keyPair, did, request)
      connectorMock.generateConnectionTokens(*, *).returns {
        ReaderT.liftF(
          IO.pure(
            List(ConnectionToken(UUID.randomUUID.toString))
          )
        )
      }
      usingApiAsContacts(rpcRequest) { serviceStub =>
        val response = serviceStub.createContact(request).contact.value
        val contactId = Contact.Id.unsafeFrom(response.contactId)
        parser.parse(response.jsonData).toOption.value must be(json)
        response.externalId must be(request.externalId)

        // the new contact needs to exist
        val result = contactsRepository
          .find(institutionId, contactId)
          .run(TraceId.generateYOLO)
          .unsafeRunSync()
        val contactWithDetails = result.value
        toContactProto(contactWithDetails.contact, connectionMissing())
          .copy(jsonData = "") must be(
          response.copy(jsonData = "")
        )
        contactWithDetails.contact.data must be(json)
      }
    }

    "create a contact without JSON data" in {
      val keyPair = EC.generateKeyPair()
      val publicKey = keyPair.getPublicKey
      val did = generateDid(publicKey)
      val institutionId = createParticipant("Institution", did)
      val externalId = Contact.ExternalId.random()

      val request = console_api
        .CreateContactRequest(
          externalId = externalId.value,
          generateConnectionTokensRequestMetadata = Some(connectorRequestMetadataProto)
        )
      val rpcRequest = SignedRpcRequest.generate(keyPair, did, request)
      connectorMock.generateConnectionTokens(*, *).returns {
        ReaderT.liftF(
          IO.pure(
            List(ConnectionToken(UUID.randomUUID.toString))
          )
        )
      }
      usingApiAsContacts(rpcRequest) { serviceStub =>
        val response = serviceStub.createContact(request).contact.value
        val contactId = Contact.Id.unsafeFrom(response.contactId)
        parser.parse(response.jsonData).toOption.value must be(Json.obj())
        response.externalId must be(request.externalId)

        // the new contact needs to exist
        val result = contactsRepository
          .find(institutionId, contactId)
          .run(TraceId.generateYOLO)
          .unsafeRunSync()
        val contactWithDetails = result.value
        toContactProto(contactWithDetails.contact, connectionMissing()) must be(
          response
        )
      }
    }

    "fail to create a contact and assign it to a group that does not exists" in {
      val keyPair = EC.generateKeyPair()
      val publicKey = keyPair.getPublicKey
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
          groupName = "missing group",
          generateConnectionTokensRequestMetadata = Some(connectorRequestMetadataProto)
        )
      val rpcRequest = SignedRpcRequest.generate(keyPair, did, request)
      connectorMock.generateConnectionTokens(*, *).returns {
        ReaderT.liftF(
          IO.pure(
            List(ConnectionToken(UUID.randomUUID.toString))
          )
        )
      }
      usingApiAsContacts(rpcRequest) { serviceStub =>
        intercept[Exception](
          serviceStub.createContact(request).contact.value
        )

        // the contact must not be added
        val result = contactsRepository
          .getBy(institutionId, Helpers.legacyQuery(None, None, 10))
          .run(TraceId.generateYOLO)
          .unsafeRunSync()
        result must be(empty)
      }
    }

    "fail on attempt to create a contact with empty external id" in {
      val keyPair = EC.generateKeyPair()
      val publicKey = keyPair.getPublicKey
      val did = generateDid(publicKey)
      val institutionId = createParticipant("Institution", did)
      val group =
        createInstitutionGroup(institutionId, InstitutionGroup.Name("group 1"))
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
          generateConnectionTokensRequestMetadata = Some(connectorRequestMetadataProto)
        )
      val rpcRequest = SignedRpcRequest.generate(keyPair, did, request)

      usingApiAsContacts(rpcRequest) { serviceStub =>
        intercept[Exception](
          serviceStub.createContact(request).contact.value
        )

        // the new contact should not exist
        val result = contactsRepository
          .getBy(institutionId, Helpers.legacyQuery(None, None, 10))
          .run(TraceId.generateYOLO)
          .unsafeRunSync()
        result must be(empty)
      }
    }

    "fail on attempt to duplicate an external id" in {
      val keyPair = EC.generateKeyPair()
      val publicKey = keyPair.getPublicKey
      val did = generateDid(publicKey)
      val institutionId = createParticipant("Institution", did)
      val group =
        createInstitutionGroup(institutionId, InstitutionGroup.Name("group 1"))
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
          externalId = externalId.value,
          generateConnectionTokensRequestMetadata = Some(connectorRequestMetadataProto)
        )
      val rpcRequest = SignedRpcRequest.generate(keyPair, did, request)
      connectorMock.generateConnectionTokens(*, *).returns {
        ReaderT.liftF(
          IO.pure(
            List(ConnectionToken(UUID.randomUUID.toString))
          )
        )
      }
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
          serviceStub
            .createContact(request.withJsonData(secondJson.noSpaces))
            .contact
            .value
        )

        // the contact needs to exist as originally inserted
        val result = contactsRepository
          .getBy(institutionId, Helpers.legacyQuery(None, None, 10))
          .run(TraceId.generateYOLO)
          .unsafeRunSync()
        result.size must be(1)

        val storedContact = result.head.details
        storedContact.data must be(json)
        storedContact.contactId.toString must be(initialResponse.contactId)
        storedContact.externalId must be(externalId)
      }
    }
  }

  "createContacts" should {

    def runTest(
        requestBuilder: ParticipantId => console_api.CreateContactsRequest
    ) = {
      val keyPair = EC.generateKeyPair()
      val publicKey = keyPair.getPublicKey
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
        .run(TraceId.generateYOLO)
        .unsafeRunSync()
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
        .withGenerateConnectionTokensRequestMetadata(
          connectorRequestMetadataProto
        )
      connectorMock.generateConnectionTokens(*, *).returns {
        ReaderT.liftF(
          IO.pure(
            List.fill(2)(ConnectionToken(UUID.randomUUID.toString))
          )
        )
      }
      val (institutionId, responseT) = runTest(_ => request)
      responseT.isSuccess must be(true)
      responseT.toOption.value.contactsCreated must be(2)
      testAvailableContacts(
        institutionId,
        responseT.toOption.value.contactsCreated
      )
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
          .withGenerateConnectionTokensRequestMetadata(
            connectorRequestMetadataProto
          )
      }
      connectorMock.generateConnectionTokens(*, *).returns {
        ReaderT.liftF(
          IO.pure(
            List.fill(2)(ConnectionToken(UUID.randomUUID.toString))
          )
        )
      }
      val (institutionId, responseT) = runTest(request)
      responseT.isSuccess must be(true)
      responseT.toOption.value.contactsCreated must be(2)

      // the new contacts need to be assigned to the groups
      groups.foreach { group =>
        institutionGroupsRepository
          .listContacts(institutionId, group)
          .run(TraceId.generateYOLO)
          .unsafeRunSync()
          .size must be(2)
      }
    }

    "fail when there are unknown groups" in {
      def request(institutionId: ParticipantId) = {
        val group1 = createInstitutionGroup(
          institutionId,
          InstitutionGroup.Name("group 1")
        )
        val group2 = createInstitutionGroup(
          institutionId,
          InstitutionGroup.Name("group 2")
        )
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
          .withGenerateConnectionTokensRequestMetadata(
            connectorRequestMetadataProto
          )
      }

      val (institutionId, responseT) = runTest(request)
      responseT.isFailure must be(true)
      testAvailableContacts(institutionId, 0)
    }

    "fail when there are repeated groups" in {
      def request(institutionId: ParticipantId) = {
        val group1 = createInstitutionGroup(
          institutionId,
          InstitutionGroup.Name("group 1")
        )
        val group2 = createInstitutionGroup(
          institutionId,
          InstitutionGroup.Name("group 2")
        )
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
          .withGenerateConnectionTokensRequestMetadata(
            connectorRequestMetadataProto
          )
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
        .withGenerateConnectionTokensRequestMetadata(
          connectorRequestMetadataProto
        )

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
        .withGenerateConnectionTokensRequestMetadata(
          connectorRequestMetadataProto
        )

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

  "updateContact" should {
    def prepare[T](
        f: (Contact.Id, ParticipantId) => console_api.UpdateContactRequest
    )(g: Try[console_api.UpdateContactResponse] => T) = {
      val keyPair = EC.generateKeyPair()
      val publicKey = keyPair.getPublicKey
      val did = generateDid(publicKey)
      val institutionId = createParticipant("Institution", did)
      val externalId = Contact.ExternalId.random()
      val json = Json.obj(
        "email" -> Json.fromString("alice@bkm.me"),
        "admissionDate" -> Json.fromString(LocalDate.now().toString)
      )
      val createRequest = console_api.CreateContactRequest(
        jsonData = json.noSpaces,
        externalId = externalId.value,
        generateConnectionTokensRequestMetadata = Some(connectorRequestMetadataProto)
      )
      val createRpcRequest =
        SignedRpcRequest.generate(keyPair, did, createRequest)

      val contactId = usingApiAsContacts(createRpcRequest) { serviceStub =>
        val str =
          serviceStub.createContact(createRequest).contact.value.contactId
        Contact.Id.from(str).toOption.value
      }

      val updateRequest = f(contactId, institutionId)
      val updateRpcRequest =
        SignedRpcRequest.generate(keyPair, did, updateRequest)
      usingApiAsContacts(updateRpcRequest) { serviceStub =>
        val response = Try {
          serviceStub.updateContact(updateRequest)
        }
        g(response)
      }
    }

    "update a contact" in {
      val newJson = Json.obj(
        "who" -> Json.fromString("me"),
        "when" -> Json.fromString(LocalDate.now().toString)
      )
      prepare((id, _) =>
        console_api.UpdateContactRequest(
          contactId = id.uuid.toString,
          newJsonData = newJson.noSpaces,
          newExternalId = ExternalId.random().toString,
          newName = "new dusty"
        )
      ) { result =>
        result.isSuccess must be(true)
      }
    }

    "work with empty name" in {
      val newJson = Json.obj(
        "who" -> Json.fromString("me"),
        "when" -> Json.fromString(LocalDate.now().toString)
      )
      prepare((id, _) =>
        console_api
          .UpdateContactRequest(
            contactId = id.uuid.toString,
            newJsonData = newJson.noSpaces,
            newExternalId = ExternalId.random().toString
          )
          .withNewName("")
      ) { result =>
        result.isSuccess must be(true)
      }
    }

    "fail when the external id is invalid" in {
      val newJson = Json.obj(
        "who" -> Json.fromString("me"),
        "when" -> Json.fromString(LocalDate.now().toString)
      )
      prepare((id, _) =>
        console_api
          .UpdateContactRequest(
            contactId = id.uuid.toString,
            newJsonData = newJson.noSpaces,
            newName = "new dusty"
          )
          .withNewExternalId("")
      ) { result =>
        result.isFailure must be(true)
      }
    }

    "fail on duplicated external id" in {
      val newJson = Json.obj(
        "who" -> Json.fromString("me"),
        "when" -> Json.fromString(LocalDate.now().toString)
      )
      def build(id: Contact.Id, institutionId: ParticipantId) = {
        val existing = createContact(institutionId, "test")

        console_api
          .UpdateContactRequest(
            contactId = id.uuid.toString,
            newJsonData = newJson.noSpaces,
            newName = "new dusty"
          )
          .withNewExternalId(existing.externalId.value)
      }
      prepare(build) { result =>
        result.isFailure must be(true)
      }
    }

    "fail on invalid json data" in {
      prepare((id, _) =>
        console_api
          .UpdateContactRequest(
            contactId = id.uuid.toString,
            newJsonData = "{",
            newName = "new dusty",
            newExternalId = Contact.ExternalId.random().toString
          )
      ) { result =>
        result.isFailure must be(true)
      }
    }
  }

  private def cleanContactData(
      c: console_models.Contact
  ): console_models.Contact = c.copy(jsonData = "")
  private def contactJsonData(c: console_models.Contact): Json =
    parser.parse(c.jsonData).toOption.value

  "getContacts" should {
    "return connectionToken of the contact if connection is not accepted" in {
      val keyPair = EC.generateKeyPair()
      val publicKey = keyPair.getPublicKey
      val did = generateDid(publicKey)
      val connectionToken = "some-connection-token"
      val institutionId = createParticipant("institutionx", did)
      val groupName = createInstitutionGroup(
        institutionId,
        InstitutionGroup.Name("Group A")
      ).name
      createContact(
        institutionId = institutionId,
        name = "Alice",
        groupName = Some(groupName),
        connectionToken = connectionToken
      )
      val request = console_api.GetContactsRequest(
        limit = 1
      )
      val rpcRequest = SignedRpcRequest.generate(keyPair, did, request)
      val connectionMissingWithToken = connectionMissing(Some(connectionToken))

      usingApiAsContacts(rpcRequest) { serviceStub =>
        connectorMock.getConnectionStatus(*).returns {
          ReaderT.liftF(
            IO.pure(
              List(connectionMissingWithToken)
            )
          )
        }
        val response = serviceStub.getContacts(request)
        val contactsReturned = response.data.flatMap(_.contact)
        contactsReturned.headOption
          .map(_.connectionToken)
          .value mustBe connectionToken
      }
    }

    "return first contacts" in {
      val keyPair = EC.generateKeyPair()
      val publicKey = keyPair.getPublicKey
      val did = generateDid(publicKey)
      val connectionToken = "some-connection-token"
      val institutionId = createParticipant("institutionx", did)
      val groupNameA = createInstitutionGroup(
        institutionId,
        InstitutionGroup.Name("Group A")
      ).name
      val groupNameB = createInstitutionGroup(
        institutionId,
        InstitutionGroup.Name("Group B")
      ).name
      val groupNameC = createInstitutionGroup(
        institutionId,
        InstitutionGroup.Name("Group C")
      ).name
      val contactA = createContact(
        institutionId = institutionId,
        name = "Alice",
        groupName = Some(groupNameA),
        connectionToken = connectionToken
      )
      val contactB = createContact(
        institutionId = institutionId,
        name = "Bob",
        groupName = Some(groupNameB),
        connectionToken = connectionToken
      )
      createContact(
        institutionId = institutionId,
        name = "Charles",
        groupName = Some(groupNameC)
      )
      createContact(
        institutionId = institutionId,
        name = "Alice 2",
        groupName = Some(groupNameA)
      )
      val request = console_api.GetContactsRequest(
        limit = 2
      )
      val rpcRequest = SignedRpcRequest.generate(keyPair, did, request)
      val connectionMissingWithToken = connectionMissing(Some(connectionToken))
      usingApiAsContacts(rpcRequest) { serviceStub =>
        connectorMock.getConnectionStatus(*).returns {
          ReaderT.liftF(
            IO.pure(
              List(connectionMissingWithToken, connectionMissingWithToken)
            )
          )
        }

        val response = serviceStub.getContacts(request)
        val contactsReturned = response.data.flatMap(_.contact)
        val contactsReturnedNoJsons = contactsReturned map cleanContactData
        val contactsReturnedJsons = contactsReturned map contactJsonData
        contactsReturnedNoJsons.toList must be(
          List(
            toContactProto(contactA, connectionMissingWithToken),
            toContactProto(contactB, connectionMissingWithToken)
          ).map(cleanContactData)
        )
        contactsReturnedJsons.toList must be(List(contactA.data, contactB.data))
      }
    }

    "return the contacts when FilterBy is provided with no criteria" in {
      val keyPair = EC.generateKeyPair()
      val publicKey = keyPair.getPublicKey
      val did = generateDid(publicKey)
      val connectionToken = "some-connection-token"
      val institutionId = createParticipant("institutionx", did)
      val groupNameA = createInstitutionGroup(
        institutionId,
        InstitutionGroup.Name("Group A")
      ).name
      val contactA = createContact(
        institutionId = institutionId,
        name = "Alice",
        connectionToken = connectionToken
      )
      val contactB = createContact(
        institutionId = institutionId,
        name = "Bob",
        groupName = Some(groupNameA),
        connectionToken = connectionToken
      )
      val request = console_api
        .GetContactsRequest(
          limit = 2
        )
        .withFilterBy(console_api.GetContactsRequest.FilterBy())
      val rpcRequest = SignedRpcRequest.generate(keyPair, did, request)
      val connectionMissingWithToken = connectionMissing(Some(connectionToken))
      usingApiAsContacts(rpcRequest) { serviceStub =>
        connectorMock.getConnectionStatus(*).returns {
          ReaderT.liftF(
            IO.pure(
              List(connectionMissingWithToken, connectionMissingWithToken)
            )
          )
        }

        val response = serviceStub.getContacts(request)
        val contactsReturned = response.data.flatMap(_.contact)
        val contactsReturnedNoJsons = contactsReturned map cleanContactData
        val contactsReturnedJsons = contactsReturned map contactJsonData
        contactsReturnedNoJsons.toList must be(
          List(
            toContactProto(contactA, connectionMissingWithToken),
            toContactProto(contactB, connectionMissingWithToken)
          ).map(cleanContactData)
        )
        contactsReturnedJsons.toList must be(List(contactA.data, contactB.data))
      }
    }

    "return first contacts matching a group" in {
      val keyPair = EC.generateKeyPair()
      val publicKey = keyPair.getPublicKey
      val did = generateDid(publicKey)
      val connectionToken = "some-connection-token"
      val institutionId = createParticipant("institutionx", did)
      val groupNameA = createInstitutionGroup(
        institutionId,
        InstitutionGroup.Name("Group A")
      ).name
      val groupNameB = createInstitutionGroup(
        institutionId,
        InstitutionGroup.Name("Group B")
      ).name
      val groupNameC = createInstitutionGroup(
        institutionId,
        InstitutionGroup.Name("Group C")
      ).name
      val contactA = createContact(
        institutionId = institutionId,
        name = "Alice",
        groupName = Some(groupNameA),
        connectionToken = connectionToken
      )
      val contactA2 = createContact(
        institutionId = institutionId,
        name = "Alice 2",
        groupName = Some(groupNameA),
        connectionToken = connectionToken
      )
      createContact(institutionId, "Bob", Some(groupNameB))
      createContact(institutionId, "Charles", Some(groupNameC))
      val request = console_api
        .GetContactsRequest(
          limit = 2
        )
        .withFilterBy(
          console_api.GetContactsRequest
            .FilterBy()
            .withGroupName(groupNameA.value)
        )
      val rpcRequest = SignedRpcRequest.generate(keyPair, did, request)
      val connectionMissingWithToken = connectionMissing(Some(connectionToken))

      usingApiAsContacts(rpcRequest) { serviceStub =>
        connectorMock.getConnectionStatus(*).returns {
          ReaderT.liftF(
            IO.pure(
              List(connectionMissingWithToken, connectionMissingWithToken)
            )
          )
        }

        val response = serviceStub.getContacts(request)
        val contactsReturned = response.data.flatMap(_.contact)
        val contactsReturnedNoJsons = contactsReturned map cleanContactData
        val contactsReturnedJsons = contactsReturned map contactJsonData
        contactsReturnedNoJsons.toList must be(
          List(
            toContactProto(contactA, connectionMissingWithToken),
            toContactProto(contactA2, connectionMissingWithToken)
          ).map(cleanContactData)
        )
        contactsReturnedJsons.toList must be(
          List(contactA.data, contactA2.data)
        )
      }
    }

    "return the first contacts matching a connection status" in {
      val keyPair = EC.generateKeyPair()
      val publicKey = keyPair.getPublicKey
      val did = generateDid(publicKey)
      val institutionId = createParticipant("institution", did)
      val groupName = createInstitutionGroup(
        institutionId,
        InstitutionGroup.Name("Group")
      ).name

      val (contactA, connectionStatusA) =
        createContactWithConnectionStatus(
          "Alice",
          "ConnectionTokenA",
          ContactConnectionStatus.STATUS_CONNECTION_ACCEPTED,
          institutionId,
          groupName
        )

      val groupNameB = createInstitutionGroup(
        institutionId,
        InstitutionGroup.Name("GroupB")
      ).name
      val (_, connectionStatusB) =
        createContactWithConnectionStatus(
          "Bob",
          "ConnectionTokenB",
          ContactConnectionStatus.STATUS_CONNECTION_ACCEPTED,
          institutionId,
          groupNameB
        )

      val (_, connectionStatusC) =
        createContactWithConnectionStatus(
          "Charles",
          "ConnectionTokenC",
          ContactConnectionStatus.STATUS_CONNECTION_REVOKED,
          institutionId,
          groupName
        )

      val (contactD, connectionStatusD) =
        createContactWithConnectionStatus(
          "Dexter",
          "ConnectionTokenD",
          ContactConnectionStatus.STATUS_CONNECTION_ACCEPTED,
          institutionId,
          groupName
        )

      val (_, connectionStatusE) =
        createContactWithConnectionStatus(
          "Ellie",
          "ConnectionTokenE",
          ContactConnectionStatus.STATUS_CONNECTION_ACCEPTED,
          institutionId,
          groupName
        )

      val request = console_api
        .GetContactsRequest(
          limit = 2
        )
        .withFilterBy(
          console_api.GetContactsRequest
            .FilterBy()
            .withGroupName(groupName.value)
            .withConnectionStatus(
              ContactConnectionStatus.STATUS_CONNECTION_ACCEPTED
            )
        )
      val rpcRequest = SignedRpcRequest.generate(keyPair, did, request)

      usingApiAsContacts(rpcRequest) { serviceStub =>
        connectorMock.getConnectionStatus(*).returns {
          ReaderT.liftF(
            IO.pure(
              List(
                connectionStatusA,
                connectionStatusB,
                connectionStatusC,
                connectionStatusD,
                connectionStatusE
              )
            )
          )
        }

        val response = serviceStub.getContacts(request)
        val contactsReturned = response.data.flatMap(_.contact)
        val contactsReturnedNoJsons = contactsReturned map cleanContactData
        val contactsReturnedJsons = contactsReturned map contactJsonData
        contactsReturnedNoJsons.toList must be(
          List(
            toContactProto(contactA, connectionStatusA),
            toContactProto(contactD, connectionStatusD)
          ).map(cleanContactData)
        )
        contactsReturnedJsons.toList must be(List(contactA.data, contactD.data))
      }
    }

    "paginate by the last seen contact" in {
      val keyPair = EC.generateKeyPair()
      val publicKey = keyPair.getPublicKey
      val did = generateDid(publicKey)
      val connectionToken = "some-connection-token"
      val institutionId = createParticipant("Institution X", did)
      val groupNameA = createInstitutionGroup(
        institutionId,
        InstitutionGroup.Name("Group A")
      ).name
      val groupNameB = createInstitutionGroup(
        institutionId,
        InstitutionGroup.Name("Group B")
      ).name
      val groupNameC = createInstitutionGroup(
        institutionId,
        InstitutionGroup.Name("Group C")
      ).name
      createContact(institutionId, "Alice", Some(groupNameA))
      createContact(institutionId, "Alice 2", Some(groupNameA))
      val contactB = createContact(
        institutionId = institutionId,
        name = "Bob",
        groupName = Some(groupNameB),
        connectionToken = connectionToken
      )
      val contactC = createContact(
        institutionId = institutionId,
        name = "Charles",
        groupName = Some(groupNameC),
        connectionToken = connectionToken
      )
      val request = console_api.GetContactsRequest(
        limit = 1,
        scrollId = contactB.contactId.toString
      )
      val rpcRequest = SignedRpcRequest.generate(keyPair, did, request)
      val connectionMissingWithToken = connectionMissing(Some(connectionToken))

      usingApiAsContacts(rpcRequest) { serviceStub =>
        connectorMock.getConnectionStatus(*).returns {
          ReaderT.liftF(
            IO.pure(
              List(connectionMissingWithToken)
            )
          )
        }

        val response = serviceStub.getContacts(request)
        val contactsReturned = response.data.flatMap(_.contact)
        val contactsReturnedNoJsons = contactsReturned map cleanContactData
        val contactsReturnedJsons = contactsReturned map contactJsonData
        contactsReturnedNoJsons.toList must be(
          List(
            cleanContactData(
              toContactProto(contactC, connectionMissingWithToken)
            )
          )
        )
        contactsReturnedJsons.toList must be(List(contactC.data))
      }
    }

    "paginate by the last seen contact matching by group" in {
      val keyPair = EC.generateKeyPair()
      val publicKey = keyPair.getPublicKey
      val did = generateDid(publicKey)
      val connectionToken = "some-connection-token"
      val institutionId = createParticipant("Institution X", did)
      val groupNameA = createInstitutionGroup(
        institutionId,
        InstitutionGroup.Name("Group A")
      ).name
      val groupNameB = createInstitutionGroup(
        institutionId,
        InstitutionGroup.Name("Group B")
      ).name
      val groupNameC = createInstitutionGroup(
        institutionId,
        InstitutionGroup.Name("Group C")
      ).name
      val contactA = createContact(
        institutionId = institutionId,
        name = "Alice",
        groupName = Some(groupNameA),
        connectionToken = connectionToken
      )
      val contactA2 = createContact(
        institutionId = institutionId,
        name = "Alice 2",
        groupName = Some(groupNameA),
        connectionToken = connectionToken
      )
      createContact(institutionId, "Bob", Some(groupNameB))
      createContact(institutionId, "Charles", Some(groupNameC))
      val request = console_api
        .GetContactsRequest(
          limit = 2,
          scrollId = contactA.contactId.toString
        )
        .withFilterBy(
          console_api.GetContactsRequest
            .FilterBy()
            .withGroupName(groupNameA.value)
        )
      val rpcRequest = SignedRpcRequest.generate(keyPair, did, request)
      val connectionMissingWithToken = connectionMissing(Some(connectionToken))

      usingApiAsContacts(rpcRequest) { serviceStub =>
        connectorMock.getConnectionStatus(*).returns {
          ReaderT.liftF(
            IO.pure(
              List(connectionMissingWithToken)
            )
          )
        }

        val response = serviceStub.getContacts(request)
        val contactsReturned = response.data.flatMap(_.contact)
        val contactsReturnedNoJsons = contactsReturned map cleanContactData
        val contactsReturnedJsons = contactsReturned map contactJsonData
        contactsReturnedNoJsons.toList must be(
          List(
            cleanContactData(
              toContactProto(contactA2, connectionMissingWithToken)
            )
          )
        )
        contactsReturnedJsons.toList must be(List(contactA2.data))
      }
    }

    "return the credential counts" in {
      val keyPair = EC.generateKeyPair()
      val publicKey = keyPair.getPublicKey
      val did = generateDid(publicKey)
      val institutionId = createParticipant("institutionx", did)
      val contactA = createContact(institutionId, "Alice")
      DataPreparation.createGenericCredential(
        institutionId,
        contactA.contactId,
        "A"
      )
      DataPreparation.createGenericCredential(
        institutionId,
        contactA.contactId,
        "B"
      )
      DataPreparation.createReceivedCredential(contactA.contactId)
      val request = console_api.GetContactsRequest(
        limit = 2
      )
      val rpcRequest = SignedRpcRequest.generate(keyPair, did, request)

      usingApiAsContacts(rpcRequest) { serviceStub =>
        connectorMock.getConnectionStatus(*).returns {
          ReaderT.liftF(
            IO.pure(
              List(connectionMissing(), connectionMissing())
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
      val publicKey = keyPair.getPublicKey
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
      val publicKey = keyPair.getPublicKey
      val did = generateDid(publicKey)
      val institutionId = createParticipant("Institution X", did)
      val groupName = createInstitutionGroup(
        institutionId,
        InstitutionGroup.Name("Group A")
      ).name
      val contact = createContact(institutionId, "Alice", Some(groupName))
      createContact(institutionId, "Bob", Some(groupName))
      val request = console_api.GetContactRequest(
        contactId = contact.contactId.toString
      )
      val rpcRequest = SignedRpcRequest.generate(keyPair, did, request)

      val contactConnection = connector_models.ContactConnection(
        connectionStatus = console_models.ContactConnectionStatus.STATUS_CONNECTION_MISSING,
        connectionToken = "connectionToken"
      )

      usingApiAsContacts(rpcRequest) { serviceStub =>
        connectorMock.getConnectionStatus(*).returns {
          ReaderT.liftF(
            IO.pure(
              List(contactConnection)
            )
          )
        }

        val response = serviceStub.getContact(request)
        cleanContactData(response.contact.value) must be(
          cleanContactData(toContactProto(contact, contactConnection))
        )
        contactJsonData(response.contact.value) must be(contact.data)
      }
    }

    "return no contact when the contact is missing (institutionId and contactId not correlated)" in {
      val institutionXId = createParticipant("Institution X")
      val keyPairY = EC.generateKeyPair()
      val publicKeyY = keyPairY.getPublicKey
      val didY = generateDid(publicKeyY)
      val institutionYId = createParticipant("Institution Y", didY)
      val groupNameA = createInstitutionGroup(
        institutionXId,
        InstitutionGroup.Name("Group A")
      ).name
      val groupNameB = createInstitutionGroup(
        institutionYId,
        InstitutionGroup.Name("Group B")
      ).name
      val contact = createContact(institutionXId, "Alice", Some(groupNameA))
      createContact(institutionYId, "Bob", Some(groupNameB))
      val request = console_api.GetContactRequest(
        contactId = contact.contactId.toString
      )
      val rpcRequest = SignedRpcRequest.generate(keyPairY, didY, request)

      usingApiAsContacts(rpcRequest) { serviceStub =>
        connectorMock.getConnectionStatus(*).returns {
          ReaderT.liftF(
            IO.pure(
              List.empty
            )
          )
        }

        val response = serviceStub.getContact(request)
        response.contact must be(empty)
      }
    }

    "return the contact with correct connection status for Issued Credentials " in {
      val keyPair = EC.generateKeyPair()
      val publicKey = keyPair.getPublicKey
      val did = generateDid(publicKey)
      val institutionId = createParticipant("Institution X", did)
      val groupName = createInstitutionGroup(
        institutionId,
        InstitutionGroup.Name("Group A")
      ).name
      val contact = createContact(institutionId, "Alice", Some(groupName))

      val issuedCredential = createGenericCredential(
        issuedBy = institutionId,
        contactId = contact.contactId,
        tag = "tag1",
        credentialIssuanceContactId = None
      )
      publishCredential(institutionId, issuedCredential)

      val request = console_api.GetContactRequest(
        contactId = contact.contactId.toString
      )
      val rpcRequest = SignedRpcRequest.generate(keyPair, did, request)

      val contactConnection = connector_models.ContactConnection(
        connectionStatus = console_models.ContactConnectionStatus.STATUS_CONNECTION_ACCEPTED,
        connectionToken = "connectionToken"
      )

      usingApiAsContacts(rpcRequest) { serviceStub =>
        connectorMock.getConnectionStatus(*).returns {
          ReaderT.liftF(
            IO.pure(
              List(contactConnection)
            )
          )
        }

        val response = serviceStub.getContact(request)
        cleanContactData(response.contact.value) must be(
          cleanContactData(toContactProto(contact, contactConnection))
        )
        contactJsonData(response.contact.value) must be(contact.data)
        response.issuedCredentials.head.connectionStatus must be(
          contactConnection.connectionStatus
        )
      }
    }

    "return the contact with revocation proof for a revoked credential" in {
      val keyPair = EC.generateKeyPair()
      val publicKey = keyPair.getPublicKey
      val did = generateDid(publicKey)
      val institutionId = createParticipant("Institution X", did)
      val groupName = createInstitutionGroup(
        institutionId,
        InstitutionGroup.Name("Group A")
      ).name
      val contact = createContact(institutionId, "Alice", Some(groupName))

      val issuedCredential = createGenericCredential(
        issuedBy = institutionId,
        contactId = contact.contactId,
        tag = "tag1",
        credentialIssuanceContactId = None
      )
      publishCredential(institutionId, issuedCredential)

      val revokedOnOperationId = AtalaOperationId.random()
      revokeCredential(institutionId, issuedCredential.credentialId, revokedOnOperationId)

      val request = console_api.GetContactRequest(
        contactId = contact.contactId.toString
      )
      val rpcRequest = SignedRpcRequest.generate(keyPair, did, request)

      val contactConnection = connector_models.ContactConnection(
        connectionStatus = console_models.ContactConnectionStatus.STATUS_CONNECTION_ACCEPTED,
        connectionToken = "connectionToken"
      )

      usingApiAsContacts(rpcRequest) { serviceStub =>
        connectorMock.getConnectionStatus(*).returns {
          ReaderT.liftF(
            IO.pure(
              List(contactConnection)
            )
          )
        }

        val response = serviceStub.getContact(request)
        response.issuedCredentials.headOption.value.revokedOnOperationId.toByteArray must be(
          revokedOnOperationId.value.toArray
        )
      }
    }

    "return the contact with empty connection status given connector returned no result " in {
      val keyPair = EC.generateKeyPair()
      val publicKey = keyPair.getPublicKey
      val did = generateDid(publicKey)
      val institutionId = createParticipant("Institution X", did)
      val groupName = createInstitutionGroup(
        institutionId,
        InstitutionGroup.Name("Group A")
      ).name
      val contact = createContact(institutionId, "Alice", Some(groupName))

      val issuedCredential = createGenericCredential(
        issuedBy = institutionId,
        contactId = contact.contactId,
        tag = "tag1",
        credentialIssuanceContactId = None
      )
      publishCredential(institutionId, issuedCredential)

      val request = console_api.GetContactRequest(
        contactId = contact.contactId.toString
      )
      val rpcRequest = SignedRpcRequest.generate(keyPair, did, request)

      val contactConnection = connector_models.ContactConnection(
        connectionStatus = console_models.ContactConnectionStatus.STATUS_CONNECTION_MISSING,
        connectionToken = "connectionToken"
      )

      usingApiAsContacts(rpcRequest) { serviceStub =>
        connectorMock.getConnectionStatus(*).returns {
          ReaderT.liftF(
            IO.pure(
              List()
            )
          )
        }

        val response = serviceStub.getContact(request)
        cleanContactData(response.contact.value) must be(
          cleanContactData(toContactProto(contact, contactConnection))
        )
        contactJsonData(response.contact.value) must be(contact.data)
        response.issuedCredentials.head.connectionStatus must be(
          contactConnection.connectionStatus
        )
      }
    }
  }

  "deleteContact" should {
    "delete the correct contact along with its credentials" in {
      val keyPair = EC.generateKeyPair()
      val publicKey = keyPair.getPublicKey
      val did = generateDid(publicKey)
      val institutionId = createParticipant("Institution X", did)
      val groupName = createInstitutionGroup(
        institutionId,
        InstitutionGroup.Name("Group A")
      ).name
      val contact = createContact(institutionId, "Alice", Some(groupName))
      val credential = createGenericCredential(institutionId, contact.contactId)
      val deleteRequest = console_api.DeleteContactRequest(
        contactId = contact.contactId.toString,
        deleteCredentials = true
      )
      val deleteRpcRequest =
        SignedRpcRequest.generate(keyPair, did, deleteRequest)

      usingApiAsContacts(deleteRpcRequest) { serviceStub =>
        connectorMock.getConnectionStatus(*).returns {
          ReaderT.liftF(
            IO.pure(
              List(connectionMissing())
            )
          )
        }

        val response = serviceStub.deleteContact(deleteRequest)
        response must be(DeleteContactResponse())
      }

      // Confirm that the contact was deleted
      checkContactExists(keyPair, did, contact) must be(false)

      // Confirm that the credential was deleted
      checkCredentialExists(keyPair, did, contact, credential) must be(false)
    }

    "do not delete a contact without deleting its credentials" in {
      val keyPair = EC.generateKeyPair()
      val publicKey = keyPair.getPublicKey
      val did = generateDid(publicKey)
      val institutionId = createParticipant("Institution X", did)
      val groupName = createInstitutionGroup(
        institutionId,
        InstitutionGroup.Name("Group A")
      ).name
      val contact = createContact(institutionId, "Alice", Some(groupName))
      val credential = createGenericCredential(institutionId, contact.contactId)
      val deleteRequest = console_api.DeleteContactRequest(
        contactId = contact.contactId.toString,
        deleteCredentials = false
      )
      val deleteRpcRequest =
        SignedRpcRequest.generate(keyPair, did, deleteRequest)

      usingApiAsContacts(deleteRpcRequest) { serviceStub =>
        connectorMock.getConnectionStatus(*).returns {
          ReaderT.liftF(
            IO.pure(
              List(connectionMissing())
            )
          )
        }

        val statusException = intercept[StatusRuntimeException] {
          serviceStub.deleteContact(deleteRequest)
        }
        statusException.getStatus.getDescription must be(
          s"Contact with id '${contact.contactId.uuid}' has some existing credentials"
        )
      }

      // Confirm that the contact was not deleted
      checkContactExists(keyPair, did, contact) must be(true)

      // Confirm that the credential was not deleted
      checkCredentialExists(keyPair, did, contact, credential) must be(true)
    }

    "do not delete a contact who belongs to the wrong institution" in {
      val realKeyPair = EC.generateKeyPair()
      val realPublicKey = realKeyPair.getPublicKey
      val realDid = generateDid(realPublicKey)
      val realInstitutionId = createParticipant("Institution X", realDid)
      val fakeKeyPair = EC.generateKeyPair()
      val fakePublicKey = fakeKeyPair.getPublicKey
      val fakeDid = generateDid(fakePublicKey)
      val fakeInstitutionId = createParticipant("Institution Y", fakeDid)
      val groupName = createInstitutionGroup(
        realInstitutionId,
        InstitutionGroup.Name("Group A")
      ).name
      val contact = createContact(realInstitutionId, "Alice", Some(groupName))
      val credential =
        createGenericCredential(realInstitutionId, contact.contactId)
      val deleteRequest = console_api.DeleteContactRequest(
        contactId = contact.contactId.toString,
        deleteCredentials = true
      )
      val deleteRpcRequest =
        SignedRpcRequest.generate(fakeKeyPair, fakeDid, deleteRequest)

      usingApiAsContacts(deleteRpcRequest) { serviceStub =>
        connectorMock.getConnectionStatus(*).returns {
          ReaderT.liftF(
            IO.pure(
              List(connectionMissing())
            )
          )
        }

        val statusException = intercept[StatusRuntimeException] {
          serviceStub.deleteContact(deleteRequest)
        }
        statusException.getStatus.getDescription must be(
          s"Contacts [${contact.contactId.uuid}] do not belong to institution ${fakeInstitutionId.uuid}"
        )
      }

      // Confirm that the contact was not deleted
      checkContactExists(realKeyPair, realDid, contact) must be(true)

      // Confirm that the credential was not deleted
      checkCredentialExists(realKeyPair, realDid, contact, credential) must be(
        true
      )
    }
  }
}
