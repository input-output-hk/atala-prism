package io.iohk.cvp.cmanager.repositories

import java.time.LocalDate

import io.circe.Json
import io.iohk.cvp.cmanager.models.{IssuerGroup, Student}
import io.iohk.cvp.cmanager.models.requests.CreateSubject
import io.iohk.cvp.cmanager.repositories.common.CManagerRepositorySpec
import io.iohk.cvp.cmanager.repositories.common.DataPreparation._
import org.scalatest.EitherValues._
import org.scalatest.OptionValues._

class IssuerSubjectsRepositorySpec extends CManagerRepositorySpec {
  lazy val repository = new IssuerSubjectsRepository(database)

  "create" should {
    "create a new subject" in {
      val issuer = createIssuer("Issuer-1").id
      val group = createIssuerGroup(issuer, IssuerGroup.Name("Grp 1"))
      val json = Json.obj(
        "universityId" -> Json.fromString("uid"),
        "name" -> Json.fromString("Dusty Here"),
        "email" -> Json.fromString("d.here@iohk.io"),
        "admissionDate" -> Json.fromString(LocalDate.now().toString)
      )
      val request = CreateSubject(issuer, group.name, json)

      val result = repository.create(request).value.futureValue
      val subject = result.right.value
      subject.groupName must be(group.name)
      subject.data must be(json)
    }
  }

  "find" should {
    "return the correct subject when present" in {
      val issuerId = createIssuer("Issuer X").id
      val groupName = createIssuerGroup(issuerId, IssuerGroup.Name("Group A")).name
      val subjectA = createSubject(issuerId, "Alice", groupName)
      createSubject(issuerId, "Bob", groupName)

      val result = repository.find(issuerId, subjectA.id).value.futureValue.right.value
      result.value must be(subjectA)
    }

    "return no subject when the subject is missing (issuerId and subjectId not correlated)" in {
      val issuerXId = createIssuer("Issuer X").id
      val issuerYId = createIssuer("Issuer Y").id
      val groupNameA = createIssuerGroup(issuerXId, IssuerGroup.Name("Group A")).name
      val groupNameB = createIssuerGroup(issuerYId, IssuerGroup.Name("Group B")).name
      val subjectA = createSubject(issuerXId, "Alice", groupNameA)
      createSubject(issuerYId, "Bob", groupNameB)

      val result = repository.find(issuerYId, subjectA.id).value.futureValue.right.value
      result must be(empty)
    }
  }

  "getBy" should {
    "return the first subjects" in {
      val issuerId = createIssuer("Issuer X").id
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
      val issuerId = createIssuer("Issuer X").id
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
      val issuerId = createIssuer("Issuer X").id
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
      val issuerId = createIssuer("Issuer X").id
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
      val issuerId = createIssuer(issuerName).id
      createIssuerGroup(issuerId, groupName)

      val subject = createSubject(issuerId, subjectName, groupName)
      val result = repository.generateToken(issuerId, subject.id).value.futureValue
      val token = result.right.value

      val updatedSubject = repository.find(issuerId, subject.id).value.futureValue.right.value.value
      updatedSubject.id must be(subject.id)
      updatedSubject.data must be(subject.data)
      updatedSubject.createdOn must be(subject.createdOn)
      updatedSubject.connectionStatus must be(Student.ConnectionStatus.ConnectionMissing)
      updatedSubject.connectionToken.value must be(token)
      updatedSubject.connectionId must be(subject.connectionId)
      updatedSubject.groupName must be(subject.groupName)
    }
  }
}
