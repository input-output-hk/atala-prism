package io.iohk.atala.prism.management.console.repositories

import cats.syntax.option._
import cats.scalatest.EitherMatchers._
import io.iohk.atala.prism.AtalaWithPostgresSpec
import io.iohk.atala.prism.management.console.DataPreparation._
import io.iohk.atala.prism.management.console.errors.{GroupNameIsNotFree, GroupsInstitutionDoNotMatch}
import io.iohk.atala.prism.management.console.models.InstitutionGroup
import org.scalatest.OptionValues._
import java.util.UUID

class InstitutionGroupsRepositorySpec extends AtalaWithPostgresSpec {
  lazy val repository = new InstitutionGroupsRepository(database)

  "create" should {
    "allow creating different groups" in {
      val groupName = InstitutionGroup.Name("IOHK 2019")
      val institutionId1 = createParticipant("Institution-1")
      val institutionId2 = createParticipant("Institution-2")

      // allows creating the same group on different institutions
      repository.create(institutionId1, groupName, Set()).value.futureValue
      repository.create(institutionId2, groupName, Set()).value.futureValue
      succeed
    }

    "fail when the group name is repeated" in {
      val institutionId = createParticipant("Institution-1")
      val groupName = InstitutionGroup.Name("IOHK 2019")

      repository.create(institutionId, groupName, Set()).value.futureValue
      intercept[Exception] {
        repository.create(institutionId, groupName, Set()).value.futureValue
      }

      succeed
    }

    "allow supplying the initial contacts list" in {
      val groupName = InstitutionGroup.Name("IOHK 2019")
      val institutionId = createParticipant("Institution-1")
      val contact1 = createContact(institutionId)
      val contact2 = createContact(institutionId)

      val result = repository
        .create(
          institutionId,
          groupName,
          Set(contact1.contactId, contact2.contactId)
        )
        .value
        .futureValue

      assert(result.isRight)

      // Check that the specified contacts were added to the group
      val contactList = repository
        .listContacts(institutionId, groupName)
        .value
        .futureValue
        .toOption
        .value

      assert(contactList.size == 2)
      contactList.toSet must be(Set(contact1, contact2))

      succeed
    }

    "fail when one of the contacts does not belong to the group institution" in {
      val groupName = InstitutionGroup.Name("IOHK 2019")
      val institutionId1 = createParticipant("Institution-1")
      val institutionId2 = createParticipant("Institution-2")
      val contact1 = createContact(institutionId1)
      val contact2 = createContact(institutionId2)

      val result = repository
        .create(
          institutionId1,
          groupName,
          Set(contact1.contactId, contact2.contactId)
        )
        .value
        .futureValue

      assert(result.isLeft)

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

  "copyGroup" should {
    "allow to copy group" in {
      val originalGroupName = InstitutionGroup.Name("IOHK 2019")
      val newGroupName = InstitutionGroup.Name("IOHK 2021")
      val institutionId = createParticipant("Institution-1")

      val maybeGroup = repository.create(institutionId, originalGroupName, Set()).value.futureValue
      maybeGroup.isRight mustBe true

      val originalGroupId = maybeGroup.toOption.get.id

      createContact(institutionId, "test-contact-1", originalGroupName.some)
      createContact(institutionId, "test-contact-2", originalGroupName.some)

      //To ensure that contacts are added
      repository.listContacts(institutionId, originalGroupName).value.futureValue.map(_.size) mustBe Right(2)

      val maybeNewGroup = repository
        .copyGroup(institutionId, originalGroupId, newGroupName)
        .value
        .futureValue

      maybeNewGroup.isRight mustBe true

      val newGroup = maybeNewGroup.toOption.get
      newGroup.name mustBe newGroupName
      newGroup.institutionId mustBe institutionId

      val maybeOriginalContacts = repository.listContacts(institutionId, originalGroupName).value.futureValue
      val maybeNewGroupContacts = repository.listContacts(institutionId, newGroupName).value.futureValue

      maybeOriginalContacts.isRight mustBe true
      maybeOriginalContacts mustBe maybeNewGroupContacts
    }

    "return error if group doesn't exists" in {
      val institutionId = createParticipant("Institution-1")
      val fakeGroupId = InstitutionGroup.Id(UUID.randomUUID())
      val newGroupName = InstitutionGroup.Name("IOHK 2021")

      repository
        .copyGroup(institutionId, fakeGroupId, newGroupName)
        .value
        .futureValue mustBe Left(GroupsInstitutionDoNotMatch(List(fakeGroupId), institutionId))
    }

    "reject group copying if there is the wrong institutionId" in {
      val groupName = InstitutionGroup.Name("IOHK 2019")
      val newGroupName = InstitutionGroup.Name("IOHK 2021")
      val institutionId = createParticipant("Institution-1")
      val imposterInstitutionId = createParticipant("Institution-2")

      val maybeGroup = repository.create(institutionId, groupName, Set()).value.futureValue
      maybeGroup.isRight mustBe true

      val group = maybeGroup.toOption.get

      repository
        .copyGroup(imposterInstitutionId, group.id, newGroupName)
        .value
        .futureValue mustBe Left(GroupsInstitutionDoNotMatch(List(group.id), imposterInstitutionId))
    }

    "copy group even without contacts" in {
      val originalGroupName = InstitutionGroup.Name("IOHK 2019")
      val newGroupName = InstitutionGroup.Name("IOHK 2021")
      val institutionId = createParticipant("Institution-1")

      val maybeGroup = repository.create(institutionId, originalGroupName, Set()).value.futureValue
      maybeGroup.isRight mustBe true

      val originalGroupId = maybeGroup.toOption.get.id

      val maybeNewGroup = repository
        .copyGroup(institutionId, originalGroupId, newGroupName)
        .value
        .futureValue

      maybeNewGroup.isRight mustBe true

      val newGroup = maybeNewGroup.toOption.get
      newGroup.name mustBe newGroupName
      newGroup.institutionId mustBe institutionId

      val maybeOriginalContacts = repository.listContacts(institutionId, originalGroupName).value.futureValue
      val maybeNewGroupContacts = repository.listContacts(institutionId, newGroupName).value.futureValue

      maybeOriginalContacts mustBe Right(Nil)
      maybeOriginalContacts mustBe maybeNewGroupContacts
    }

    "reject copying if the new name already taken by another group" in {
      val originalGroupName = InstitutionGroup.Name("IOHK 2019")
      val group2Name = InstitutionGroup.Name("IOHK 2021")
      val institutionId = createParticipant("Institution-1")

      val maybeGroup = repository.create(institutionId, originalGroupName, Set()).value.futureValue
      maybeGroup.isRight mustBe true
      repository.create(institutionId, group2Name, Set()).value.futureValue.isRight mustBe true

      val originalGroupId = maybeGroup.toOption.get.id

      repository
        .copyGroup(institutionId, originalGroupId, group2Name)
        .value
        .futureValue mustBe Left(GroupNameIsNotFree(group2Name))
    }
  }

  "deleteGroup" should {
    "allow to delete group" in {
      val groupName = InstitutionGroup.Name("IOHK 2019")
      val institutionId = createParticipant("Institution-1")

      val maybeGroup = repository.create(institutionId, groupName, Set()).value.futureValue
      maybeGroup.isRight mustBe true

      val groupId = maybeGroup.toOption.get.id

      createContact(institutionId, "test-contact-1", groupName.some)
      createContact(institutionId, "test-contact-2", groupName.some)

      //To ensure that contacts are added
      repository.listContacts(institutionId, groupName).value.futureValue.map(_.size) mustBe Right(2)

      repository.deleteGroup(institutionId, groupId).value.futureValue.isRight mustBe true
      repository.getBy(institutionId, None).value.futureValue mustBe Right(Nil)
      //Guarantee that we removed contacts and group
      intercept[RuntimeException](repository.listContacts(institutionId, groupName).value.futureValue)
      succeed
    }

    "return error if group doesn't exists" in {
      val institutionId = createParticipant("Institution-1")
      val fakeGroupId = InstitutionGroup.Id(UUID.randomUUID())

      repository
        .deleteGroup(institutionId, fakeGroupId)
        .value
        .futureValue mustBe Left(GroupsInstitutionDoNotMatch(List(fakeGroupId), institutionId))
    }

    "reject group deleting if there is wrong institutionId" in {
      val groupName = InstitutionGroup.Name("IOHK 2019")
      val institutionId = createParticipant("Institution-1")
      val imposterInstitutionId = createParticipant("Institution-2")

      val maybeGroup = repository.create(institutionId, groupName, Set()).value.futureValue
      maybeGroup.isRight mustBe true

      val group = maybeGroup.toOption.get

      repository
        .deleteGroup(imposterInstitutionId, group.id)
        .value
        .futureValue mustBe Left(GroupsInstitutionDoNotMatch(List(group.id), imposterInstitutionId))
    }
  }
}
