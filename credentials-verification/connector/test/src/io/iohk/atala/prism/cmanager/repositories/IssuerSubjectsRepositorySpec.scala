package io.iohk.atala.prism.cmanager.repositories

import java.time.LocalDate

import io.circe.Json
import io.iohk.atala.prism.cmanager.models.IssuerGroup
import io.iohk.atala.prism.cmanager.repositories.common.CManagerRepositorySpec
import io.iohk.atala.prism.cmanager.repositories.common.DataPreparation._
import io.iohk.atala.prism.console.models.{Contact, CreateContact, Institution}
import org.scalatest.EitherValues._
import org.scalatest.OptionValues._

class IssuerSubjectsRepositorySpec extends CManagerRepositorySpec {
  lazy val repository = new IssuerSubjectsRepository(database)

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
      val subject = result.right.value
      subject.data must be(json)
      subject.externalId must be(externalId)
      subject.connectionToken must be(empty)
      subject.connectionId must be(empty)

      // we check that the subject was added to the intended group
      val subjectsInGroupList = repository.getBy(issuer, 10, None, Some(group.name)).value.futureValue.right.value
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
      val subject = result.right.value
      subject.data must be(json)
      subject.externalId must be(externalId)
      subject.connectionToken must be(empty)
      subject.connectionId must be(empty)

      // we check that the subject was added
      val maybeSubject = repository.find(issuer, subject.id).value.futureValue.right.value.value
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
      val subjectsList = repository.getBy(issuerId, 1, None, None).value.futureValue.right.value
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
      val createdSubjects = repository.getBy(issuerId, 10, None, None).value.futureValue.right.value
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

      val initialResponse = repository.create(request, Some(group.name)).value.futureValue.right.value

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

      val subjectsStored = repository.getBy(issuerId, 10, None, None).value.futureValue.right.value

      // only one subject must be inserted correctly
      subjectsStored.size must be(1)

      val subject = subjectsStored.head
      // the subject must have the original data
      subject.data must be(json)
      subject.id must be(initialResponse.id)
      subject.externalId must be(externalId)
    }
  }

  "find by subjectId" should {
    "return the correct subject when present" in {
      val issuerId = createIssuer("Issuer X")
      val groupName = createIssuerGroup(issuerId, IssuerGroup.Name("Group A")).name
      val subjectA = createSubject(issuerId, "Alice", groupName)
      createSubject(issuerId, "Bob", groupName)

      val result = repository.find(issuerId, subjectA.id).value.futureValue.right.value
      result.value must be(subjectA)
    }

    "return no subject when the subject is missing (issuerId and subjectId not correlated)" in {
      val issuerXId = createIssuer("Issuer X")
      val issuerYId = createIssuer("Issuer Y")
      val groupNameA = createIssuerGroup(issuerXId, IssuerGroup.Name("Group A")).name
      val groupNameB = createIssuerGroup(issuerYId, IssuerGroup.Name("Group B")).name
      val subjectA = createSubject(issuerXId, "Alice", groupNameA)
      createSubject(issuerYId, "Bob", groupNameB)

      val result = repository.find(issuerYId, subjectA.id).value.futureValue.right.value
      result must be(empty)
    }
  }

  "find by externalId" should {
    "return the correct subject when present" in {
      val issuerId = createIssuer("Issuer X")
      val subjectA = createSubject(issuerId, "Alice", None, "subject-1")
      createSubject(issuerId, "Bob", None, "subject-2")

      val result = repository.find(issuerId, subjectA.externalId).value.futureValue.right.value
      result.value must be(subjectA)
    }

    "return no subject when the subject is missing (issuerId and subjectId not correlated)" in {
      val issuerXId = createIssuer("Issuer X")
      val issuerYId = createIssuer("Issuer Y")
      val groupNameA = createIssuerGroup(issuerXId, IssuerGroup.Name("Group A")).name
      val groupNameB = createIssuerGroup(issuerYId, IssuerGroup.Name("Group B")).name
      val subjectA = createSubject(issuerXId, "Alice", groupNameA)
      createSubject(issuerYId, "Bob", groupNameB)

      val result = repository.find(issuerYId, subjectA.externalId).value.futureValue.right.value
      result must be(empty)
    }
  }

  "getBy" should {
    "return the first subjects" in {
      val issuerId = createIssuer("Issuer X")
      val groupNameA = createIssuerGroup(issuerId, IssuerGroup.Name("Group A")).name
      val groupNameB = createIssuerGroup(issuerId, IssuerGroup.Name("Group B")).name
      val groupNameC = createIssuerGroup(issuerId, IssuerGroup.Name("Group C")).name
      val subjectA = createSubject(issuerId, "Alice", groupNameA)
      val subjectB = createSubject(issuerId, "Bob", groupNameB)
      createSubject(issuerId, "Charles", groupNameC)
      createSubject(issuerId, "Alice 2", groupNameA)

      val result = repository.getBy(issuerId, 2, None, None).value.futureValue.right.value
      result must be(List(subjectA, subjectB))
    }

    "return the first subjects matching a group" in {
      val issuerId = createIssuer("Issuer X")
      val groupNameA = createIssuerGroup(issuerId, IssuerGroup.Name("Group A")).name
      val groupNameB = createIssuerGroup(issuerId, IssuerGroup.Name("Group B")).name
      val groupNameC = createIssuerGroup(issuerId, IssuerGroup.Name("Group C")).name
      val subjectA = createSubject(issuerId, "Alice", groupNameA)
      createSubject(issuerId, "Bob", groupNameB)
      createSubject(issuerId, "Charles", groupNameC)
      val subjectA2 = createSubject(issuerId, "Alice 2", groupNameA)

      val result = repository.getBy(issuerId, 2, None, Some(groupNameA)).value.futureValue.right.value
      result must be(List(subjectA, subjectA2))
    }

    "paginate by the last seen subject" in {
      val issuerId = createIssuer("Issuer X")
      val groupNameA = createIssuerGroup(issuerId, IssuerGroup.Name("Group A")).name
      val groupNameB = createIssuerGroup(issuerId, IssuerGroup.Name("Group B")).name
      val groupNameC = createIssuerGroup(issuerId, IssuerGroup.Name("Group C")).name
      createSubject(issuerId, "Alice", groupNameA)
      val subjectB = createSubject(issuerId, "Bob", groupNameB)
      val subjectC = createSubject(issuerId, "Charles", groupNameC)
      createSubject(issuerId, "Alice 2", groupNameA)

      val result = repository.getBy(issuerId, 1, Some(subjectB.id), None).value.futureValue.right.value
      result must be(List(subjectC))
    }

    "paginate by the last seen subject matching by group" in {
      val issuerId = createIssuer("Issuer X")
      val groupNameA = createIssuerGroup(issuerId, IssuerGroup.Name("Group A")).name
      val groupNameB = createIssuerGroup(issuerId, IssuerGroup.Name("Group B")).name
      val groupNameC = createIssuerGroup(issuerId, IssuerGroup.Name("Group C")).name
      val subjectA = createSubject(issuerId, "Alice", groupNameA)
      createSubject(issuerId, "Bob", groupNameB)
      createSubject(issuerId, "Charles", groupNameC)
      val subjectA2 = createSubject(issuerId, "Alice 2", groupNameA)

      val result = repository
        .getBy(issuerId, 1, Some(subjectA.id), Some(groupNameA))
        .value
        .futureValue
        .right
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

      val subject = createSubject(issuerId, subjectName, groupName)
      val result = repository.generateToken(Institution.Id(issuerId.value), subject.id).value.futureValue
      val token = result.right.value

      val updatedSubject = repository.find(issuerId, subject.id).value.futureValue.right.value.value
      updatedSubject.id must be(subject.id)
      updatedSubject.data must be(subject.data)
      updatedSubject.createdAt must be(subject.createdAt)
      updatedSubject.connectionStatus must be(Contact.ConnectionStatus.ConnectionMissing)
      updatedSubject.connectionToken.value must be(token)
      updatedSubject.connectionId must be(subject.connectionId)
    }
  }
}
