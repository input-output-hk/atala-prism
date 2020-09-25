package io.iohk.atala.prism.cmanager.grpc.services

import java.time.LocalDate
import java.util.UUID

import io.circe
import io.circe.{Json, parser}
import io.iohk.atala.prism.connector.model.TokenString
import io.iohk.atala.prism.connector.repositories.{ParticipantsRepository, RequestNoncesRepository}
import io.iohk.atala.prism.connector.{RpcSpecBase, SignedRequestsAuthenticator}
import io.iohk.atala.prism.cmanager.grpc.services.codecs.ProtoCodecs.{genericCredentialToProto, subjectToProto}
import io.iohk.atala.prism.cmanager.repositories._
import io.iohk.atala.prism.cmanager.repositories.common.DataPreparation._
import io.iohk.atala.prism.console.models.{Contact, Institution, IssuerGroup}
import io.iohk.atala.prism.console.repositories.ContactsRepository
import io.iohk.atala.prism.grpc.GrpcAuthenticationHeaderParser
import io.iohk.atala.prism.models.ParticipantId
import io.iohk.prism.protos.cmanager_api
import io.iohk.prism.protos.cmanager_api.{GetSubjectCredentialsRequest, GetSubjectRequest, GetSubjectsRequest}
import io.iohk.prism.protos.cmanager_models.{CManagerGenericCredential, IssuerSubject}
import org.mockito.MockitoSugar._
import org.scalatest.EitherValues._
import org.scalatest.OptionValues._

import scala.concurrent.duration.DurationDouble

class SubjectsServiceImplSpec extends RpcSpecBase {

  private implicit val executionContext = scala.concurrent.ExecutionContext.global
  private implicit val pc: PatienceConfig = PatienceConfig(20.seconds, 20.millis)
  private val usingApiAs = usingApiAsConstructor(new cmanager_api.SubjectsServiceGrpc.SubjectsServiceBlockingStub(_, _))

  private lazy val participantsRepository = new ParticipantsRepository(database)
  private lazy val contactsRepository = new ContactsRepository(database)
  private lazy val credentialsRepository = new CredentialsRepository(database)
  private lazy val requestNoncesRepository = new RequestNoncesRepository.PostgresImpl(database)(executionContext)
  private lazy val nodeMock = mock[io.iohk.prism.protos.node_api.NodeServiceGrpc.NodeService]
  private lazy val authenticator = new SignedRequestsAuthenticator(
    participantsRepository,
    requestNoncesRepository,
    nodeMock,
    GrpcAuthenticationHeaderParser
  )

  override def services =
    Seq(
      cmanager_api.SubjectsServiceGrpc
        .bindService(
          new SubjectsServiceImpl(contactsRepository, credentialsRepository, authenticator),
          executionContext
        )
    )

  "createSubject" should {
    "create a subject and assign it to a group" in {
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

        val request = cmanager_api
          .CreateSubjectRequest(
            groupName = group.name.value,
            jsonData = json.noSpaces,
            externalId = externalId.value
          )

        val response = serviceStub.createSubject(request).subject.value
        response.groupName must be(empty)
        parser.parse(response.jsonData).right.value must be(json)
        response.externalId must be(request.externalId)

        // the new subject needs to exist
        val result = contactsRepository.getBy(issuerId, None, Some(group.name), 10).value.futureValue.right.value
        result.size must be(1)
        val storedSubject = result.headOption.value
        subjectToProto(storedSubject).copy(jsonData = "") must be(response.copy(jsonData = ""))
        storedSubject.data must be(json)
      }
    }

    "create a subject and assign it to no group" in {
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

        val request = cmanager_api
          .CreateSubjectRequest(
            jsonData = json.noSpaces,
            externalId = externalId.value
          )

        val response = serviceStub.createSubject(request).subject.value
        val subjectId = Contact.Id(UUID.fromString(response.id))
        response.groupName must be(empty)
        parser.parse(response.jsonData).right.value must be(json)
        response.externalId must be(request.externalId)

        // the new subject needs to exist
        val result = contactsRepository.find(issuerId, subjectId).value.futureValue.right.value
        val storedSubject = result.value
        subjectToProto(storedSubject).copy(jsonData = "") must be(response.copy(jsonData = ""))
        storedSubject.data must be(json)
      }
    }

    "fail to create a subject and assign it to a group that does not exists" in {
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

        val request = cmanager_api
          .CreateSubjectRequest(
            jsonData = json.noSpaces,
            externalId = externalId.value,
            groupName = "missing group"
          )

        intercept[Exception](
          serviceStub.createSubject(request).subject.value
        )

        // the subject must not be added
        val result = contactsRepository.getBy(issuerId, None, None, 10).value.futureValue.right.value
        result must be(empty)
      }
    }

    // TODO: Remove ignore when the front end provides the external id
    "fail on attempt to create a subject with empty external id" ignore {
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

        val request = cmanager_api
          .CreateSubjectRequest(
            groupName = group.name.value,
            jsonData = json.noSpaces
          )

        intercept[Exception](
          serviceStub.createSubject(request).subject.value
        )

        // the new subject should not exist
        val result = contactsRepository.getBy(issuerId, None, None, 10).value.futureValue.right.value
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

        val request = cmanager_api
          .CreateSubjectRequest(
            groupName = group.name.value,
            jsonData = json.noSpaces,
            externalId = externalId.value
          )

        val initialResponse = serviceStub.createSubject(request).subject.value

        // We attempt to insert another subject with the same external id
        val secondJson = Json
          .obj(
            "universityAssignedId" -> Json.fromString("noneyet"),
            "fullName" -> Json.fromString("Second Subject"),
            "email" -> Json.fromString("second@test.me"),
            "admissionDate" -> Json.fromString(LocalDate.now().toString)
          )

        intercept[Exception](
          serviceStub.createSubject(request.withJsonData(secondJson.noSpaces)).subject.value
        )

        // the subject needs to exist as originally inserted
        val result = contactsRepository.getBy(issuerId, None, None, 10).value.futureValue.right.value
        result.size must be(1)

        val storedSubject = result.head
        storedSubject.data must be(json)
        storedSubject.contactId.value.toString must be(initialResponse.id)
        storedSubject.externalId must be(externalId)
      }
    }
  }

  private def cleanSubjectData(is: IssuerSubject): IssuerSubject = is.copy(jsonData = "")
  private def subjectJsonData(is: IssuerSubject): Json = circe.parser.parse(is.jsonData).right.value
  private def cleanCredentialData(gc: CManagerGenericCredential): CManagerGenericCredential =
    gc.copy(credentialData = "", subjectData = "")
  private def credentialJsonData(gc: CManagerGenericCredential): (Json, Json) =
    (circe.parser.parse(gc.credentialData).right.value, circe.parser.parse(gc.subjectData).right.value)

  "getSubjects" should {
    "return the first subjects" in {
      val issuerId = createIssuer("Issuer X")
      val groupNameA = createIssuerGroup(issuerId, IssuerGroup.Name("Group A")).name
      val groupNameB = createIssuerGroup(issuerId, IssuerGroup.Name("Group B")).name
      val groupNameC = createIssuerGroup(issuerId, IssuerGroup.Name("Group C")).name
      val subjectA = createContact(issuerId, "Alice", groupNameA)
      val subjectB = createContact(issuerId, "Bob", groupNameB)
      createContact(issuerId, "Charles", groupNameC)
      createContact(issuerId, "Alice 2", groupNameA)

      usingApiAs(toParticipantId(issuerId)) { serviceStub =>
        val request = GetSubjectsRequest(
          limit = 2
        )

        val response = serviceStub.getSubjects(request)
        val subjectsReturned = response.subjects
        val subjectsReturnedNoJsons = subjectsReturned map cleanSubjectData
        val subjectsReturnedJsons = subjectsReturned map subjectJsonData
        subjectsReturnedNoJsons.toList must be(
          List(subjectToProto(subjectA), subjectToProto(subjectB)) map cleanSubjectData
        )
        subjectsReturnedJsons.toList must be(List(subjectA.data, subjectB.data))
      }
    }

    "return the first subjects matching a group" in {
      val issuerId = createIssuer("Issuer X")
      val groupNameA = createIssuerGroup(issuerId, IssuerGroup.Name("Group A")).name
      val groupNameB = createIssuerGroup(issuerId, IssuerGroup.Name("Group B")).name
      val groupNameC = createIssuerGroup(issuerId, IssuerGroup.Name("Group C")).name
      val subjectA = createContact(issuerId, "Alice", groupNameA)
      createContact(issuerId, "Bob", groupNameB)
      createContact(issuerId, "Charles", groupNameC)
      val subjectA2 = createContact(issuerId, "Alice 2", groupNameA)

      usingApiAs(toParticipantId(issuerId)) { serviceStub =>
        val request = GetSubjectsRequest(
          limit = 2,
          groupName = groupNameA.value
        )

        val response = serviceStub.getSubjects(request)
        val subjectsReturned = response.subjects
        val subjectsReturnedNoJsons = subjectsReturned map cleanSubjectData
        val subjectsReturnedJsons = subjectsReturned map subjectJsonData
        subjectsReturnedNoJsons.toList must be(
          List(subjectToProto(subjectA), subjectToProto(subjectA2)) map cleanSubjectData
        )
        subjectsReturnedJsons.toList must be(List(subjectA.data, subjectA2.data))
      }
    }

    "paginate by the last seen subject" in {
      val issuerId = createIssuer("Issuer X")
      val groupNameA = createIssuerGroup(issuerId, IssuerGroup.Name("Group A")).name
      val groupNameB = createIssuerGroup(issuerId, IssuerGroup.Name("Group B")).name
      val groupNameC = createIssuerGroup(issuerId, IssuerGroup.Name("Group C")).name
      createContact(issuerId, "Alice", groupNameA)
      val subjectB = createContact(issuerId, "Bob", groupNameB)
      val subjectC = createContact(issuerId, "Charles", groupNameC)
      createContact(issuerId, "Alice 2", groupNameA)

      usingApiAs(toParticipantId(issuerId)) { serviceStub =>
        val request = GetSubjectsRequest(
          limit = 1,
          lastSeenSubjectId = subjectB.contactId.value.toString
        )

        val response = serviceStub.getSubjects(request)
        val subjectsReturned = response.subjects
        val subjectsReturnedNoJsons = subjectsReturned map cleanSubjectData
        val subjectsReturnedJsons = subjectsReturned map subjectJsonData
        subjectsReturnedNoJsons.toList must be(List(cleanSubjectData(subjectToProto(subjectC))))
        subjectsReturnedJsons.toList must be(List(subjectC.data))
      }
    }

    "paginate by the last seen subject matching by group" in {
      val issuerId = createIssuer("Issuer X")
      val groupNameA = createIssuerGroup(issuerId, IssuerGroup.Name("Group A")).name
      val groupNameB = createIssuerGroup(issuerId, IssuerGroup.Name("Group B")).name
      val groupNameC = createIssuerGroup(issuerId, IssuerGroup.Name("Group C")).name
      val subjectA = createContact(issuerId, "Alice", groupNameA)
      createContact(issuerId, "Bob", groupNameB)
      createContact(issuerId, "Charles", groupNameC)
      val subjectA2 = createContact(issuerId, "Alice 2", groupNameA)

      usingApiAs(toParticipantId(issuerId)) { serviceStub =>
        val request = GetSubjectsRequest(
          limit = 2,
          lastSeenSubjectId = subjectA.contactId.value.toString,
          groupName = groupNameA.value
        )

        val response = serviceStub.getSubjects(request)
        val subjectsReturned = response.subjects
        val subjectsReturnedNoJsons = subjectsReturned map cleanSubjectData
        val subjectsReturnedJsons = subjectsReturned map subjectJsonData
        subjectsReturnedNoJsons.toList must be(List(cleanSubjectData(subjectToProto(subjectA2))))
        subjectsReturnedJsons.toList must be(List(subjectA2.data))
      }
    }
  }

  "getSubject" should {
    "return the correct subject when present" in {
      val issuerId = createIssuer("Issuer X")
      val groupName = createIssuerGroup(issuerId, IssuerGroup.Name("Group A")).name
      val subject = createContact(issuerId, "Alice", groupName)
      createContact(issuerId, "Bob", groupName)

      usingApiAs(toParticipantId(issuerId)) { serviceStub =>
        val request = GetSubjectRequest(
          subjectId = subject.contactId.value.toString
        )

        val response = serviceStub.getSubject(request)
        cleanSubjectData(response.subject.value) must be(cleanSubjectData(subjectToProto(subject)))
        subjectJsonData(response.subject.value) must be(subject.data)
      }
    }

    "return no subject when the subject is missing (issuerId and subjectId not correlated)" in {
      val issuerXId = createIssuer("Issuer X")
      val issuerYId = createIssuer("Issuer Y")
      val groupNameA = createIssuerGroup(issuerXId, IssuerGroup.Name("Group A")).name
      val groupNameB = createIssuerGroup(issuerYId, IssuerGroup.Name("Group B")).name
      val subject = createContact(issuerXId, "Alice", groupNameA)
      createContact(issuerYId, "Bob", groupNameB)

      usingApiAs(toParticipantId(issuerYId)) { serviceStub =>
        val request = GetSubjectRequest(
          subjectId = subject.contactId.value.toString
        )

        val response = serviceStub.getSubject(request)
        response.subject must be(empty)
      }
    }
  }

  "getSubjectCredentials" should {
    "return subject's credentials" in {
      val issuerId = createIssuer("Issuer X")
      val group = createIssuerGroup(issuerId, IssuerGroup.Name("grp1"))
      val subjectId1 = createContact(issuerId, "IOHK Student", group.name).contactId
      val subjectId2 = createContact(issuerId, "IOHK Student 2", group.name).contactId
      createGenericCredential(issuerId, subjectId2, "A")
      val cred1 = createGenericCredential(issuerId, subjectId1, "B")
      createGenericCredential(issuerId, subjectId2, "C")
      val cred2 = createGenericCredential(issuerId, subjectId1, "D")
      createGenericCredential(issuerId, subjectId2, "E")

      usingApiAs(toParticipantId(issuerId)) { serviceStub =>
        val request = GetSubjectCredentialsRequest(
          subjectId = subjectId1.value.toString
        )

        val response = serviceStub.getSubjectCredentials(request)
        val returnedCredentials = response.genericCredentials.toList
        val cleanCredentials = returnedCredentials map cleanCredentialData
        val credentialsJsons = returnedCredentials map credentialJsonData

        val expectedCredentials = List(cred1, cred2)
        val expectedCleanCredentials = expectedCredentials map {
          genericCredentialToProto _ andThen cleanCredentialData
        }
        val expectedCredentialsJsons = expectedCredentials map { genericCredentialToProto _ andThen credentialJsonData }
        cleanCredentials must be(expectedCleanCredentials)
        credentialsJsons must be(expectedCredentialsJsons)
      }
    }

    "return empty list of credentials when not present" in {
      val issuerId = createIssuer("Issuer X")
      val group = createIssuerGroup(issuerId, IssuerGroup.Name("grp1"))
      val subjectId = createContact(issuerId, "IOHK Student", group.name).contactId

      usingApiAs(toParticipantId(issuerId)) { serviceStub =>
        val request = GetSubjectCredentialsRequest(
          subjectId = subjectId.value.toString
        )

        val response = serviceStub.getSubjectCredentials(request)
        response.genericCredentials must be(empty)
      }
    }
  }

  "generateConnectionTokenForSubject" should {
    "generate a token" in {
      val issuerName = "tokenizer"
      val groupName = IssuerGroup.Name("Grp 1")
      val subjectName = "Subject 1"
      val issuerId = createIssuer(issuerName)
      createIssuerGroup(issuerId, groupName)
      val subject = createContact(issuerId, subjectName, groupName)

      usingApiAs(toParticipantId(issuerId)) { serviceStub =>
        val request = cmanager_api
          .GenerateConnectionTokenForSubjectRequest(
            subjectId = subject.contactId.value.toString
          )

        val response = serviceStub.generateConnectionTokenForSubject(request)
        val token = TokenString(response.token)

        // the new subject needs to exist
        val result = contactsRepository.find(issuerId, subject.contactId).value.futureValue.right.value
        val storedSubject = result.value
        storedSubject.contactId must be(subject.contactId)
        storedSubject.data must be(subject.data)
        storedSubject.createdAt must be(subject.createdAt)
        storedSubject.connectionStatus must be(Contact.ConnectionStatus.ConnectionMissing)
        storedSubject.connectionToken.value must be(token)
        storedSubject.connectionId must be(subject.connectionId)
      }
    }
  }

  private def toParticipantId(issuer: Institution.Id): ParticipantId = {
    ParticipantId(issuer.value)
  }
}
