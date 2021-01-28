package io.iohk.atala.prism.management.console.repositories

import io.iohk.atala.prism.AtalaWithPostgresSpec
import io.iohk.atala.prism.management.console.DataPreparation._
import org.scalatest.OptionValues._

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

  trait Fixtures {
    val institution1 = createParticipant("Institution-1")
    val institution2 = createParticipant("Institution-2")

    val credentialType1 = createCredentialType(institution1, "name1")
    val credentialType2 = createCredentialType(institution1, "name2")
    val credentialType3 = createCredentialType(institution2, "name1")
    val credentialType4 = createCredentialType(institution2, "name2")
  }
}
