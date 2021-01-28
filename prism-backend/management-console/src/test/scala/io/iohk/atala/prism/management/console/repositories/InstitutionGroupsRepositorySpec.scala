package io.iohk.atala.prism.management.console.repositories

import cats.scalatest.EitherMatchers._
import io.iohk.atala.prism.AtalaWithPostgresSpec
import io.iohk.atala.prism.management.console.DataPreparation._
import io.iohk.atala.prism.management.console.models.InstitutionGroup
import org.scalatest.OptionValues._

class InstitutionGroupsRepositorySpec extends AtalaWithPostgresSpec {
  lazy val repository = new InstitutionGroupsRepository(database)

  "create" should {
    "allow creating different groups" in {
      val groupName = InstitutionGroup.Name("IOHK 2019")
      val institutionId1 = createParticipant("Institution-1")
      val institutionId2 = createParticipant("Institution-2")

      // allows creating the same group on different institutions
      repository.create(institutionId1, groupName).value.futureValue
      repository.create(institutionId2, groupName).value.futureValue
      succeed
    }

    "fail when the group name is repeated" in {
      val institutionId = createParticipant("Institution-1")
      val groupName = InstitutionGroup.Name("IOHK 2019")

      repository.create(institutionId, groupName).value.futureValue
      intercept[Exception] {
        repository.create(institutionId, groupName).value.futureValue
      }

      succeed
    }
  }

  "getBy" should {
    "get the available groups for an institution" in {
      val groups = List("Group 1", "Group 2").map(InstitutionGroup.Name.apply)
      val institutionId1 = createParticipant("Institution-1")
      createInstitutionGroup(institutionId1, groups(0))
      createInstitutionGroup(institutionId1, groups(1))

      val institutionId2 = createParticipant("Institution-2")
      createInstitutionGroup(institutionId2, InstitutionGroup.Name("Other"))

      val result = repository.getBy(institutionId1, None).value.futureValue.toOption.value
      result.map(_.value.name) must be(groups)
    }

    "includes the contact count" in {
      val groups = List("Group 1", "Group 2").map(InstitutionGroup.Name.apply)
      val institutionId = createParticipant("Institution-1")
      createInstitutionGroup(institutionId, groups(0))
      createInstitutionGroup(institutionId, groups(1))
      createContact(institutionId, "test-contact-1", groups.headOption)
      createContact(institutionId, "test-contact-2", groups.headOption)
      createContact(institutionId, "test-contact-3", groups.lift(1))

      val result = repository.getBy(institutionId, None).value.futureValue
      result.map(_.map(_.numberOfContacts)) must beRight(List(2, 1))
    }

    "allows filtering by contact" in {
      val groups = List("Group 1", "Group 2").map(InstitutionGroup.Name.apply)
      val issuerId = createParticipant("Institution-1")
      groups.foreach { g => createInstitutionGroup(issuerId, g) }
      createContact(issuerId, "test-contact-1", groups.headOption)
      val contact = createContact(issuerId, "test-contact-2", groups.headOption)
      createContact(issuerId, "test-contact-3", groups.lift(1))

      val result = repository.getBy(issuerId, Some(contact.contactId)).value.futureValue.toOption.value
      result.size must be(1)

      val resultGroup = result.head
      resultGroup.value.name must be(groups(0))
      resultGroup.numberOfContacts must be(2)
    }
  }
}
