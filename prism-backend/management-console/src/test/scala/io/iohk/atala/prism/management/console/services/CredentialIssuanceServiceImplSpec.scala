package io.iohk.atala.prism.management.console.services

import io.circe.{Json, JsonObject, parser}
import io.iohk.atala.prism.DIDUtil
import io.iohk.atala.prism.auth.SignedRpcRequest
import io.iohk.atala.prism.crypto.EC.{INSTANCE => EC}
import io.iohk.atala.prism.management.console.DataPreparation._
import io.iohk.atala.prism.management.console.models.{Contact, CredentialTypeId, InstitutionGroup, ParticipantId}
import io.iohk.atala.prism.management.console.{DataPreparation, ManagementConsoleRpcSpecBase}
import io.iohk.atala.prism.protos.{console_api, console_models}
import org.mockito.IdiomaticMockito._
import org.scalatest.OptionValues._

import java.time.Instant
import java.util.UUID
import io.circe.syntax.EncoderOps
import io.iohk.atala.prism.logging.TraceId

//sbt "project management-console" "testOnly *CredentialIssuanceServiceImplSpec"
class CredentialIssuanceServiceImplSpec extends ManagementConsoleRpcSpecBase with DIDUtil {

  "createCredentialIssuance and getCredentialIssuance" should {
    val keyPair = EC.generateKeyPair()
    val did = generateDid(keyPair.getPublicKey)
    val otherKeyPair = EC.generateKeyPair()
    val otherDid = generateDid(otherKeyPair.getPublicKey)

    def createCredentialIssuanceRequest(
        contacts: List[console_models.CredentialIssuanceContact],
        credentialTypeId: CredentialTypeId
    ): console_api.CreateCredentialIssuanceRequest = {
      console_api.CreateCredentialIssuanceRequest(
        name = "2021 Class",
        credentialTypeId = credentialTypeId.uuid.toString,
        credentialIssuanceContacts = contacts
      )
    }

    "create and get a credential issuance" in {
      val institutionId = createParticipant("Institution", did)
      val contacts = createRandomCredentialIssuanceContacts(institutionId)

      val credentialTypeWithRequiredFields =
        DataPreparation.createCredentialType(institutionId, "name")

      // Create the credential issuance
      val createRequest = createCredentialIssuanceRequest(
        contacts,
        credentialTypeWithRequiredFields.credentialType.id
      )
      usingApiAsCredentialIssuance(
        SignedRpcRequest.generate(keyPair, did, createRequest)
      ) { serviceStub =>
        val creationTime = Instant.now
        val createResponse = serviceStub.createCredentialIssuance(createRequest)

        // Verify the generated credentials
        val credentials =
          credentialsRepository
            .getBy(institutionId, contacts.size + 1, None)
            .run(TraceId.generateYOLO)
            .unsafeRunSync()
            .sortBy(_.contactId.toString)
        credentials.size mustBe contacts.size
        contacts.zip(credentials).foreach { case (contact, credential) =>
          credential.contactId.toString mustBe contact.contactId
          credential.credentialData mustBe asJson(contact.credentialData)
          credential.createdOn must (be >= creationTime and be <= Instant.now)
        }

        // Get the credential issuance just created and verify it matches
        val getRequest =
          console_api.GetCredentialIssuanceRequest(credentialIssuanceId = createResponse.credentialIssuanceId)
        usingApiAsCredentialIssuance(
          SignedRpcRequest.generate(keyPair, did, getRequest)
        ) { serviceStub =>
          val credentialIssuance = serviceStub.getCredentialIssuance(getRequest)

          // Verify the obtained credential issuance matches the created one
          credentialIssuance.name mustBe createRequest.name
          credentialIssuance.credentialTypeId mustBe createRequest.credentialTypeId
          credentialIssuance.createdAt.fold(0L)(
            _.seconds
          ) must (be >= creationTime.getEpochSecond and be <= Instant.now.getEpochSecond)
          // Verify contacts
          val issuanceContacts =
            credentialIssuance.credentialIssuanceContacts.sortBy(_.contactId)
          issuanceContacts.size mustBe contacts.size
          contacts.zip(issuanceContacts).foreach { case (contact, issuanceContact) =>
            issuanceContact.contactId mustBe contact.contactId
            issuanceContact.groupIds must contain theSameElementsAs contact.groupIds
            asJson(issuanceContact.credentialData) mustBe asJson(
              contact.credentialData
            )
          }
        }
      }
    }

    "fail to create for a contact outside the institution" in {
      val institutionId = createParticipant("Institution", did)
      val contacts = createRandomCredentialIssuanceContacts(institutionId)
      val otherInstitutionId = createParticipant("Other Institution", otherDid)
      val otherContacts =
        List(createRandomCredentialIssuanceContact(otherInstitutionId))
      val credentialTypeWithRequiredFields =
        DataPreparation.createCredentialType(institutionId, "name")

      val createRequest =
        createCredentialIssuanceRequest(
          contacts ++ otherContacts,
          credentialTypeWithRequiredFields.credentialType.id
        )
      usingApiAsCredentialIssuance(
        SignedRpcRequest.generate(keyPair, did, createRequest)
      ) { serviceStub =>
        assertThrows[Exception] {
          serviceStub.createCredentialIssuance(createRequest)
        }
      }
    }

    "fail to get for a nonexistent ID" in {
      createParticipant("Institution", did)

      val getRequest =
        console_api.GetCredentialIssuanceRequest(credentialIssuanceId = UUID.randomUUID().toString)
      usingApiAsCredentialIssuance(
        SignedRpcRequest.generate(keyPair, did, getRequest)
      ) { serviceStub =>
        assertThrows[Exception] {
          serviceStub.getCredentialIssuance(getRequest)
        }
      }
    }
  }

  "createGenericCredentialBulk and getCredentialIssuance" should {
    val keyPair = EC.generateKeyPair()
    val did = generateDid(keyPair.getPublicKey)
    val otherKeyPair = EC.generateKeyPair()
    val otherDid = generateDid(otherKeyPair.getPublicKey)

    val issuanceName = "2021 Class"

    def createBulk(
        issuanceName: String,
        credentialType: CredentialTypeId,
        contacts: List[
          (Contact.ExternalId, console_models.CredentialIssuanceContact)
        ]
    ): console_api.CreateGenericCredentialBulkRequest = {
      def toJson(
          contact: (
              Contact.ExternalId,
              console_models.CredentialIssuanceContact
          )
      ): Json = {
        JsonObject(
          "external_id" -> contact._1.value.asJson,
          "credential_data" -> asJson(contact._2.credentialData),
          "group_ids" -> Json.fromValues(contact._2.groupIds.map(_.asJson))
        ).asJson
      }

      val json =
        JsonObject(
          "issuance_name" -> issuanceName.asJson,
          "credential_type_id" -> credentialType.uuid.toString.asJson,
          "drafts" -> Json.fromValues(contacts.map(toJson).toVector)
        )

      console_api.CreateGenericCredentialBulkRequest(json.asJson.noSpaces)
    }

    "create and get a credential issuance" in {
      val institutionId = createParticipant("Institution", did)
      val contactsWithExternalIds =
        createRandomCredentialIssuanceContactsWithExternalId(institutionId)
      val contacts = contactsWithExternalIds.map(_._2)

      val credentialTypeWithRequiredFields =
        DataPreparation.createCredentialType(institutionId, "name")

      // Create the credential issuance
      val createRequest =
        createBulk(
          issuanceName,
          credentialTypeWithRequiredFields.credentialType.id,
          contactsWithExternalIds
        )
      usingApiAsCredentialIssuance(
        SignedRpcRequest.generate(keyPair, did, createRequest)
      ) { serviceStub =>
        val creationTime = Instant.now
        val createResponse =
          serviceStub.createGenericCredentialBulk(createRequest)

        // Verify the generated credentials
        val credentials =
          credentialsRepository
            .getBy(institutionId, contacts.size + 1, None)
            .run(TraceId.generateYOLO)
            .unsafeRunSync()
            .sortBy(_.contactId.toString)
        credentials.size mustBe contacts.size
        contacts.zip(credentials).foreach { case (contact, credential) =>
          credential.contactId.toString mustBe contact.contactId
          credential.credentialData mustBe asJson(contact.credentialData)
          credential.createdOn must (be >= creationTime and be <= Instant.now)
        }

        // Get the credential issuance just created and verify it matches
        val getRequest =
          console_api.GetCredentialIssuanceRequest(credentialIssuanceId = createResponse.credentialIssuanceId)
        usingApiAsCredentialIssuance(
          SignedRpcRequest.generate(keyPair, did, getRequest)
        ) { serviceStub =>
          val credentialIssuance = serviceStub.getCredentialIssuance(getRequest)

          // Verify the obtained credential issuance matches the created one
          credentialIssuance.name mustBe issuanceName
          credentialIssuance.credentialTypeId mustBe credentialTypeWithRequiredFields.credentialType.id.uuid.toString
          credentialIssuance.createdAt.fold(0L)(
            _.seconds
          ) must (be >= creationTime.getEpochSecond and be <= Instant.now.getEpochSecond)
          // Verify contacts
          val issuanceContacts =
            credentialIssuance.credentialIssuanceContacts.sortBy(_.contactId)
          issuanceContacts.size mustBe contacts.size
          contacts.zip(issuanceContacts).foreach { case (contact, issuanceContact) =>
            issuanceContact.contactId mustBe contact.contactId
            issuanceContact.groupIds must contain theSameElementsAs contact.groupIds
            asJson(issuanceContact.credentialData) mustBe asJson(
              contact.credentialData
            )
          }
        }
      }
    }

    "fail to create for a contact outside the institution" in {
      val institutionId = createParticipant("Institution", did)
      val contacts =
        createRandomCredentialIssuanceContactsWithExternalId(institutionId)
      val otherInstitutionId = createParticipant("Other Institution", otherDid)
      val otherContacts = List(
        createRandomCredentialIssuanceContactWithExternalId(otherInstitutionId)
      )
      val credentialTypeWithRequiredFields =
        DataPreparation.createCredentialType(institutionId, "name")

      val createRequest =
        createBulk(
          issuanceName,
          credentialTypeWithRequiredFields.credentialType.id,
          contacts ++ otherContacts
        )
      usingApiAsCredentialIssuance(
        SignedRpcRequest.generate(keyPair, did, createRequest)
      ) { serviceStub =>
        assertThrows[Exception] {
          serviceStub.createGenericCredentialBulk(createRequest)
        }
      }
    }
  }

  private def createRandomCredentialIssuanceContacts(
      institutionId: ParticipantId
  ): List[console_models.CredentialIssuanceContact] = {
    val groups = List("Engineering", "Business").map { groupName =>
      institutionGroupsRepository
        .create(institutionId, InstitutionGroup.Name(groupName), Set())
        .run(TraceId.generateYOLO)
        .unsafeRunSync()
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
    val contact = DataPreparation.createContact(
      institutionId,
      groupName = group.map(_.name)
    )
    val contactId = contact.contactId.toString
    console_models.CredentialIssuanceContact(
      contactId = contactId,
      credentialData = Json
        .obj(
          "title" -> Json.fromString("Major IN Applied Blockchain"),
          "enrollmentDate" -> Json.fromString("01/10/2010"),
          "graduationDate" -> Json.fromString("01/07/2015")
        )
        .toString(),
      groupIds = group.map(_.id.toString).toList
    )
  }

  // with external ids
  private def createRandomCredentialIssuanceContactsWithExternalId(
      institutionId: ParticipantId
  ): List[(Contact.ExternalId, console_models.CredentialIssuanceContact)] = {
    val groups = List("Engineering", "Business").map { groupName =>
      institutionGroupsRepository
        .create(institutionId, InstitutionGroup.Name(groupName), Set())
        .run(TraceId.generateYOLO)
        .unsafeRunSync()
        .toOption
        .value
    }
    val contactsWithGroup =
      groups.map { group =>
        createRandomCredentialIssuanceContactWithExternalId(
          institutionId,
          Some(group)
        )
      }
    val contactsWithoutGroup = (1 to 2).map { _ =>
      createRandomCredentialIssuanceContactWithExternalId(institutionId)
    }

    (contactsWithGroup ++ contactsWithoutGroup).sortBy(_._2.contactId)
  }

  private def createRandomCredentialIssuanceContactWithExternalId(
      institutionId: ParticipantId,
      group: Option[InstitutionGroup] = None
  ): (Contact.ExternalId, console_models.CredentialIssuanceContact) = {
    val contact = DataPreparation.createContact(
      institutionId,
      groupName = group.map(_.name)
    )
    val contactId = contact.contactId.toString
    (
      contact.externalId,
      console_models.CredentialIssuanceContact(
        contactId = contactId,
        credentialData = Json
          .obj(
            "contactId" -> Json.fromString(contactId),
            "title" -> Json.fromString("Major IN Applied Blockchain"),
            "enrollmentDate" -> Json.fromString("01/10/2010"),
            "graduationDate" -> Json.fromString("01/07/2015")
          )
          .toString(),
        groupIds = group.map(_.id.toString).toList
      )
    )
  }

  private def asJson(string: String): Json = parser.parse(string).toOption.value
}
