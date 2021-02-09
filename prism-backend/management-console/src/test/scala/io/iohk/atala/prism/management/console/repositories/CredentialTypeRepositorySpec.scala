package io.iohk.atala.prism.management.console.repositories

import io.iohk.atala.prism.AtalaWithPostgresSpec
import io.iohk.atala.prism.management.console.DataPreparation._
import io.iohk.atala.prism.management.console.models.{
  CreateCredentialTypeField,
  CredentialTypeId,
  CredentialTypeState,
  UpdateCredentialType
}
import org.scalatest.OptionValues._

import java.util.UUID

//sbt "project management-console" "testOnly *CredentialTypeRepositorySpec"
class CredentialTypeRepositorySpec extends AtalaWithPostgresSpec {
  lazy val repository = new CredentialTypeRepository(database)

  "create" should {
    "create a new credential type with required fields" in {
      val institutionId = createParticipant("Institution-1")

      val createCredentialType = sampleCreateCredentialType(institutionId, "name")

      val result = repository.create(createCredentialType).value.futureValue.toOption.value

      val credentialType = repository.find(result.credentialType.id).value.futureValue.toOption.value
      credentialType mustBe a[Some[_]]
    }

    "do not create another credential type with the same name within organization" in {
      val institutionId = createParticipant("Institution-1")
      val templateName = "name"
      createCredentialType(institutionId, templateName)

      intercept[Exception](
        repository.create(sampleCreateCredentialType(institutionId, templateName)).value.futureValue
      )
    }
  }

  "find" should {
    "find by id" in new Fixtures {
      val credentialType = repository.find(credentialType3.credentialType.id).value.futureValue.toOption.value

      credentialType mustBe Some(credentialType3)
    }

    "find by institution and name" in new Fixtures {
      val credentialType = repository
        .find(credentialType3.credentialType.institution, credentialType3.credentialType.name)
        .value
        .futureValue
        .toOption
        .value

      credentialType mustBe Some(credentialType3)
    }

    "find by institution and id" in new Fixtures {
      val credentialType = repository
        .find(credentialType3.credentialType.institution, credentialType3.credentialType.id)
        .value
        .futureValue
        .toOption
        .value

      credentialType mustBe Some(credentialType3)
    }

    "find by institution" in new Fixtures {
      val credentialTypes = repository
        .findByInstitution(institution2)
        .value
        .futureValue
        .toOption
        .value

      credentialTypes mustBe List(credentialType3, credentialType4).map(_.credentialType)
    }
  }

  "update" should {
    "update credential type" in new Fixtures {
      val updateRequest = prepareUpdateCredentialType(credentialType1.credentialType.id)

      val result = repository.update(updateRequest, institution1).value.futureValue
      val updatedCredentialType = repository.find(credentialType1.credentialType.id).value.futureValue.toOption.value

      result mustBe a[Right[_, _]]
      updatedCredentialType.isDefined mustBe true
      updatedCredentialType.foreach { credentialType =>
        credentialType.credentialType.name mustBe updateRequest.name
        credentialType.credentialType.template mustBe updateRequest.template
        credentialType.requiredFields.size mustBe updateRequest.fields.size
        credentialType.requiredFields.sortBy(_.name).zip(updateRequest.fields.sortBy(_.name)).foreach {
          case (updatedCredentialTypeField, field) =>
            updatedCredentialTypeField.name mustBe field.name
            updatedCredentialTypeField.description mustBe field.description
        }
      }
    }

    "return error when credential type does not exist" in new Fixtures {
      val updateRequest = prepareUpdateCredentialType(CredentialTypeId(UUID.randomUUID()))

      val result = repository.update(updateRequest, institution1).value.futureValue
      result mustBe a[Left[_, _]]
    }

    "return error when updated credential type is not in draft state" in new Fixtures {
      repository.markAsReady(credentialType1.credentialType.id, institution1).value.futureValue

      val updateRequest = prepareUpdateCredentialType(credentialType1.credentialType.id)

      val result = repository.update(updateRequest, institution1).value.futureValue
      result mustBe a[Left[_, _]]
    }

    "return error when updated credential does not belong to institution which initiated update" in new Fixtures {
      val updateRequest = prepareUpdateCredentialType(credentialType1.credentialType.id)

      val result = repository.update(updateRequest, institution2).value.futureValue
      result mustBe a[Left[_, _]]
    }
  }

  "mark as archived" should {
    "change state of credential type to archive" in new Fixtures {
      val result = repository.markAsArchived(credentialType1.credentialType.id, institution1).value.futureValue
      val updatedCredentialType = repository.find(credentialType1.credentialType.id).value.futureValue.toOption.value

      result mustBe a[Right[_, _]]
      updatedCredentialType.map(_.credentialType.state) mustBe Some(CredentialTypeState.Archived)
    }

    "return error when credential type does not exist" in new Fixtures {
      val result = repository.markAsArchived(CredentialTypeId(UUID.randomUUID()), institution1).value.futureValue
      result mustBe a[Left[_, _]]
    }

    "return error when updated credential does not belong to institution which initiated update" in new Fixtures {
      val result = repository.markAsArchived(credentialType1.credentialType.id, institution2).value.futureValue
      result mustBe a[Left[_, _]]
    }
  }

  "mark as ready" should {
    "change state of credential type to ready" in new Fixtures {
      val result = repository.markAsReady(credentialType1.credentialType.id, institution1).value.futureValue
      val updatedCredentialType = repository.find(credentialType1.credentialType.id).value.futureValue.toOption.value

      result mustBe a[Right[_, _]]
      updatedCredentialType.map(_.credentialType.state) mustBe Some(CredentialTypeState.Ready)
    }

    "return error when credential type does not exist" in new Fixtures {
      val result = repository.markAsReady(CredentialTypeId(UUID.randomUUID()), institution1).value.futureValue
      result mustBe a[Left[_, _]]
    }

    "return error when credential type has already been marked as archived" in new Fixtures {
      repository.markAsArchived(credentialType1.credentialType.id, institution1).value.futureValue

      val result = repository.markAsReady(credentialType1.credentialType.id, institution1).value.futureValue
      result mustBe a[Left[_, _]]
    }

    "return error when updated credential does not belong to institution which initiated update" in new Fixtures {
      val result = repository.markAsReady(credentialType1.credentialType.id, institution2).value.futureValue
      result mustBe a[Left[_, _]]
    }
  }

  trait Fixtures {
    val institution1 = createParticipant("Institution-1")
    val institution2 = createParticipant("Institution-2")

    val credentialType1 = createCredentialType(institution1, "name1")
    val credentialType2 = createCredentialType(institution1, "name2")
    val credentialType3 = createCredentialType(institution2, "name1")
    val credentialType4 = createCredentialType(institution2, "name2")
  }

  def prepareUpdateCredentialType(credentialTypeId: CredentialTypeId): UpdateCredentialType = {
    UpdateCredentialType(
      id = credentialTypeId,
      name = "new name",
      template = "new template",
      fields = 1
        .to(3)
        .map(id =>
          CreateCredentialTypeField(
            name = s"new name $id",
            description = s"new description $id"
          )
        )
        .toList
    )
  }
}
