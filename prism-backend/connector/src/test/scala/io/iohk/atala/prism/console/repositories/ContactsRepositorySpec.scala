package io.iohk.atala.prism.console.repositories

import java.time.LocalDate
import io.circe.Json
import io.iohk.atala.prism.connector.model.ConnectionStatus
import io.iohk.atala.prism.console.DataPreparation._
import io.iohk.atala.prism.console.models.{Contact, CreateContact, Institution, IssuerGroup}
import io.iohk.atala.prism.repositories.PostgresRepositorySpec
import org.scalatest.OptionValues._

import scala.concurrent.duration._

class ContactsRepositorySpec extends PostgresRepositorySpec {

  implicit val pc: PatienceConfig = PatienceConfig(20.seconds, 5.millis)

  lazy val repository = new ContactsRepository(database)

  "create" should {
    "create a new subject and assign it to an specified group" in {
      val issuer = createIssuer("Issuer-1")
      val group = createIssuerGroup(issuer, IssuerGroup.Name("Grp 1"))
      val externalId = Contact.ExternalId.random()
      val json = Json.obj(
        "universityId" -> Json.fromString("uid"),
        "name" -> Json.fromString("Dusty Here"),
        "email" -> Json.fromString("d.here@iohk.io"),
        "admissionDate" -> Json.fromString(LocalDate.now().toString)
      )
      val request = CreateContact(issuer, externalId, json)

      val result = repository.create(request, Some(group.name)).value.futureValue
      val subject = result.toOption.value
      subject.data must be(json)
      subject.externalId must be(externalId)
      subject.connectionToken must be(empty)
      subject.connectionId must be(empty)

      // we check that the subject was added to the intended group
      val subjectsInGroupList = repository.getBy(issuer, None, Some(group.name), 10).value.futureValue.toOption.value
      subjectsInGroupList.size must be(1)
      subjectsInGroupList.headOption.value must be(subject)
    }

    "create a new subject and assign it to no specified group" in {
      val issuer = createIssuer("Issuer-1")
      val externalId = Contact.ExternalId.random()
      val json = Json.obj(
        "universityId" -> Json.fromString("uid"),
        "name" -> Json.fromString("Dusty Here"),
        "email" -> Json.fromString("d.here@iohk.io"),
        "admissionDate" -> Json.fromString(LocalDate.now().toString)
      )
      val request = CreateContact(issuer, externalId, json)

      val result = repository.create(request, None).value.futureValue
      val subject = result.toOption.value
      subject.data must be(json)
      subject.externalId must be(externalId)
      subject.connectionToken must be(empty)
      subject.connectionId must be(empty)

      // we check that the subject was added
      val maybeSubject = repository.find(issuer, subject.contactId).value.futureValue.toOption.value.value
      maybeSubject must be(subject)
    }

    "fail to create a new subject when the specified group does not exist" in {
      val issuerId = createIssuer("Issuer-1")
      val externalId = Contact.ExternalId.random()
      val json = Json.obj(
        "universityId" -> Json.fromString("uid"),
        "name" -> Json.fromString("Dusty Here"),
        "email" -> Json.fromString("d.here@iohk.io"),
        "admissionDate" -> Json.fromString(LocalDate.now().toString)
      )
      val request = CreateContact(issuerId, externalId, json)

      intercept[Exception](
        repository.create(request, Some(IssuerGroup.Name("Grp 1"))).value.futureValue
      )

      // we check that the subject was not created
      val subjectsList = repository.getBy(issuerId, None, None, 1).value.futureValue.toOption.value
      subjectsList must be(empty)
    }

    "fail to create a new subject with empty external id" in {
      val issuerId = createIssuer("Issuer-1")
      val group = createIssuerGroup(issuerId, IssuerGroup.Name("Grp 1"))
      val externalId = Contact.ExternalId("")
      val json = Json.obj(
        "universityId" -> Json.fromString("uid"),
        "name" -> Json.fromString("Dusty Here"),
        "email" -> Json.fromString("d.here@iohk.io"),
        "admissionDate" -> Json.fromString(LocalDate.now().toString)
      )
      val request = CreateContact(issuerId, externalId, json)

      intercept[Exception](
        repository.create(request, Some(group.name)).value.futureValue
      )
      // no subject should be created
      val createdSubjects = repository.getBy(issuerId, None, None, 10).value.futureValue.toOption.value
      createdSubjects must be(empty)
    }

    "fail to create a new subject with an external id already used" in {
      val issuerId = createIssuer("Issuer-1")
      val group = createIssuerGroup(issuerId, IssuerGroup.Name("Grp 1"))
      val externalId = Contact.ExternalId.random()
      val json = Json.obj(
        "universityId" -> Json.fromString("uid"),
        "name" -> Json.fromString("Dusty Here"),
        "email" -> Json.fromString("d.here@iohk.io"),
        "admissionDate" -> Json.fromString(LocalDate.now().toString)
      )
      val request = CreateContact(issuerId, externalId, json)

      val initialResponse = repository.create(request, Some(group.name)).value.futureValue.toOption.value

      val secondJson = Json.obj(
        "universityId" -> Json.fromString("uid"),
        "name" -> Json.fromString("Dusty Here"),
        "email" -> Json.fromString("d.here@iohk.io"),
        "admissionDate" -> Json.fromString(LocalDate.now().toString)
      )

      val secondRequest = CreateContact(issuerId, externalId, secondJson)

      intercept[Exception](
        repository.create(secondRequest, Some(group.name)).value.futureValue
      )

      val subjectsStored = repository.getBy(issuerId, None, None, 10).value.futureValue.toOption.value

      // only one subject must be inserted correctly
      subjectsStored.size must be(1)

      val subject = subjectsStored.head
      // the subject must have the original data
      subject.data must be(json)
      subject.contactId must be(initialResponse.contactId)
      subject.externalId must be(externalId)
    }
  }

  "find by subjectId" should {
    "return the correct subject when present" in {
      val issuerId = createIssuer("Issuer X")
      val groupName = createIssuerGroup(issuerId, IssuerGroup.Name("Group A")).name
      val subjectA = createContact(issuerId, "Alice", groupName)
      createContact(issuerId, "Bob", groupName)

      val result = repository.find(issuerId, subjectA.contactId).value.futureValue.toOption.value
      result.value must be(subjectA)
    }

    "return no subject when the subject is missing (issuerId and subjectId not correlated)" in {
      val issuerXId = createIssuer("Issuer X")
      val issuerYId = createIssuer("Issuer Y")
      val groupNameA = createIssuerGroup(issuerXId, IssuerGroup.Name("Group A")).name
      val groupNameB = createIssuerGroup(issuerYId, IssuerGroup.Name("Group B")).name
      val subjectA = createContact(issuerXId, "Alice", groupNameA)
      createContact(issuerYId, "Bob", groupNameB)

      val result = repository.find(issuerYId, subjectA.contactId).value.futureValue.toOption.value
      result must be(empty)
    }
  }

  "find by externalId" should {
    "return the correct subject when present" in {
      val issuerId = createIssuer("Issuer X")
      val subjectA = createContact(issuerId, "Alice", None, "subject-1")
      createContact(issuerId, "Bob", None, "subject-2")

      val result = repository.find(issuerId, subjectA.externalId).value.futureValue.toOption.value
      result.value must be(subjectA)
    }

    "return no subject when the subject is missing (issuerId and subjectId not correlated)" in {
      val issuerXId = createIssuer("Issuer X")
      val issuerYId = createIssuer("Issuer Y")
      val groupNameA = createIssuerGroup(issuerXId, IssuerGroup.Name("Group A")).name
      val groupNameB = createIssuerGroup(issuerYId, IssuerGroup.Name("Group B")).name
      val subjectA = createContact(issuerXId, "Alice", groupNameA)
      createContact(issuerYId, "Bob", groupNameB)

      val result = repository.find(issuerYId, subjectA.externalId).value.futureValue.toOption.value
      result must be(empty)
    }
  }

  "getBy" should {
    "return the first subjects" in {
      val issuerId = createIssuer("Issuer X")
      val groupNameA = createIssuerGroup(issuerId, IssuerGroup.Name("Group A")).name
      val groupNameB = createIssuerGroup(issuerId, IssuerGroup.Name("Group B")).name
      val groupNameC = createIssuerGroup(issuerId, IssuerGroup.Name("Group C")).name
      val subjectA = createContact(issuerId, "Alice", groupNameA)
      val subjectB = createContact(issuerId, "Bob", groupNameB)
      createContact(issuerId, "Charles", groupNameC)
      createContact(issuerId, "Alice 2", groupNameA)

      val result = repository.getBy(issuerId, None, None, 2).value.futureValue.toOption.value
      result must be(List(subjectA, subjectB))
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

      val result = repository.getBy(issuerId, None, Some(groupNameA), 2).value.futureValue.toOption.value
      result must be(List(subjectA, subjectA2))
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

      val result = repository.getBy(issuerId, Some(subjectB.contactId), None, 1).value.futureValue.toOption.value
      result must be(List(subjectC))
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

      val result = repository
        .getBy(issuerId, Some(subjectA.contactId), Some(groupNameA), 1)
        .value
        .futureValue
        .toOption
        .value
      result must be(List(subjectA2))
    }
  }

  "generateToken" should {
    "update the subject to set the status and token" in {
      val issuerName = "tokenizer"
      val groupName = IssuerGroup.Name("Grp 1")
      val subjectName = "Subject 1"
      val issuerId = createIssuer(issuerName)
      createIssuerGroup(issuerId, groupName)

      val subject = createContact(issuerId, subjectName, groupName)
      val result = repository.generateToken(Institution.Id(issuerId.value), subject.contactId).value.futureValue
      val token = result.toOption.value

      val updatedSubject = repository.find(issuerId, subject.contactId).value.futureValue.toOption.value.value
      updatedSubject.contactId must be(subject.contactId)
      updatedSubject.data must be(subject.data)
      updatedSubject.createdAt must be(subject.createdAt)
      updatedSubject.connectionStatus must be(ConnectionStatus.ConnectionMissing)
      updatedSubject.connectionToken.value must be(token)
      updatedSubject.connectionId must be(subject.connectionId)
    }
  }
}
