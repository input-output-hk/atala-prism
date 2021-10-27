package io.iohk.atala.prism.management.console.repositories

import cats.effect.IO
import cats.syntax.option._
import io.iohk.atala.prism.AtalaWithPostgresSpec
import io.iohk.atala.prism.management.console.DataPreparation._
import io.iohk.atala.prism.management.console.errors.{GroupNameIsNotFree, GroupsInstitutionDoNotMatch}
import io.iohk.atala.prism.management.console.models.PaginatedQueryConstraints.ResultOrdering
import io.iohk.atala.prism.management.console.models.{InstitutionGroup, PaginatedQueryConstraints}
import io.iohk.atala.prism.utils.IOUtils._
import tofu.logging.Logs

import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.UUID
import scala.util.{Success, Try}

class InstitutionGroupsRepositorySpec extends AtalaWithPostgresSpec {
  val logs: Logs[IO, IO] = Logs.sync[IO, IO]
  lazy val repository = InstitutionGroupsRepository.unsafe(database, logs)

  "create" should {
    "allow creating different groups" in {
      val groupName = InstitutionGroup.Name("IOHK 2019")
      val institutionId1 = createParticipant("Institution-1")
      val institutionId2 = createParticipant("Institution-2")

      // allows creating the same group on different institutions
      repository.create(institutionId1, groupName, Set()).unsafeRunSync()
      repository.create(institutionId2, groupName, Set()).unsafeRunSync()
      succeed
    }

    "fail when the group name is repeated" in {
      val institutionId = createParticipant("Institution-1")
      val groupName = InstitutionGroup.Name("IOHK 2019")

      repository.create(institutionId, groupName, Set()).unsafeRunSync()
      intercept[Exception] {
        repository.create(institutionId, groupName, Set()).unsafeRunSync()
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
        .unsafeRunSync()

      assert(result.isRight)

      // Check that the specified contacts were added to the group
      val contactList = repository
        .listContacts(institutionId, groupName)
        .unsafeRunSync()

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
        .unsafeRunSync()

      assert(result.isLeft)

      succeed
    }
  }

  "getBy" should {
    val beforeDate = LocalDate.now().minus(5, ChronoUnit.DAYS)
    val afterDate = LocalDate.now().plus(5, ChronoUnit.DAYS)

    "get the available groups for an institution with total number of groups" in {
      val groups = List("Group 1", "Group 2").map(InstitutionGroup.Name.apply)
      val institutionId1 = createParticipant("Institution-1")
      createInstitutionGroup(institutionId1, groups(0))
      createInstitutionGroup(institutionId1, groups(1))

      val institutionId2 = createParticipant("Institution-2")
      createInstitutionGroup(institutionId2, InstitutionGroup.Name("Other"))

      val query: InstitutionGroup.PaginatedQuery =
        PaginatedQueryConstraints(ordering = ResultOrdering(InstitutionGroup.SortBy.Name))

      val result =
        repository.getBy(institutionId1, query).unsafeRunSync()
      result.groups.map(_.value.name) must be(groups)
      result.totalNumberOfRecords mustBe groups.size
    }

    "includes the contact count" in {
      val groups = List("Group 1", "Group 2").map(InstitutionGroup.Name.apply)
      val institutionId = createParticipant("Institution-1")
      createInstitutionGroup(institutionId, groups(0))
      createInstitutionGroup(institutionId, groups(1))
      createContact(institutionId, "test-contact-1", groups.headOption)
      createContact(institutionId, "test-contact-2", groups.headOption)
      createContact(institutionId, "test-contact-3", groups.lift(1))

      val query: InstitutionGroup.PaginatedQuery =
        PaginatedQueryConstraints(ordering = ResultOrdering(InstitutionGroup.SortBy.Name))

      val result =
        repository.getBy(institutionId, query).unsafeRunSync().groups
      result.map(_.numberOfContacts) mustBe List(2, 1)
    }

    "allows filtering by contact" in {
      val groups = List("Group 1", "Group 2").map(InstitutionGroup.Name.apply)
      val issuerId = createParticipant("Institution-1")
      groups.foreach { g => createInstitutionGroup(issuerId, g) }
      createContact(issuerId, "test-contact-1", groups.headOption)
      val contact = createContact(issuerId, "test-contact-2", groups.headOption)
      createContact(issuerId, "test-contact-3", groups.lift(1))

      val query: InstitutionGroup.PaginatedQuery =
        PaginatedQueryConstraints(
          ordering = ResultOrdering(InstitutionGroup.SortBy.Name),
          filters = Some(InstitutionGroup.FilterBy(contactId = Some(contact.contactId)))
        )

      val result = repository.getBy(issuerId, query).unsafeRunSync().groups
      result.size must be(1)

      val resultGroup = result.head
      resultGroup.value.name must be(groups.head)
      resultGroup.numberOfContacts must be(2)
    }

    "allow filtering by name" in {
      val groupName = "nAme 3"
      val groups =
        List("Group 1", "Group 2", groupName).map(InstitutionGroup.Name.apply)
      val institutionId = createParticipant("Institution-1")
      groups.foreach { g => createInstitutionGroup(institutionId, g) }

      def testName(name: String) = {
        val query: InstitutionGroup.PaginatedQuery =
          PaginatedQueryConstraints(
            ordering = ResultOrdering(InstitutionGroup.SortBy.Name),
            filters = Some(
              InstitutionGroup.FilterBy(name = Some(InstitutionGroup.Name(name)))
            )
          )

        val result =
          repository.getBy(institutionId, query).unsafeRunSync().groups
        result.size mustBe 1
        result.head.value.name.value mustBe groupName
      }

      testName("am")
      testName("Am")
      testName("Me")
    }

    "allow filtering by created after" in {
      val groups = List("Group 1", "Group 2").map(InstitutionGroup.Name.apply)
      val institutionId = createParticipant("Institution-1")
      groups.foreach { g => createInstitutionGroup(institutionId, g) }

      def testDate(date: LocalDate, expectedCount: Int) = {
        val query: InstitutionGroup.PaginatedQuery =
          PaginatedQueryConstraints(
            ordering = ResultOrdering(InstitutionGroup.SortBy.Name),
            filters = Some(InstitutionGroup.FilterBy(createdAfter = Some(date)))
          )

        val result =
          repository.getBy(institutionId, query).unsafeRunSync().groups
        result.size mustBe expectedCount
      }

      testDate(beforeDate, expectedCount = 2)
      testDate(afterDate, expectedCount = 0)
    }

    "allow filtering by created before" in {
      val groups = List("Group 1", "Group 2").map(InstitutionGroup.Name.apply)
      val institutionId = createParticipant("Institution-1")
      groups.foreach { g => createInstitutionGroup(institutionId, g) }

      def testDate(date: LocalDate, expectedCount: Int) = {
        val query: InstitutionGroup.PaginatedQuery =
          PaginatedQueryConstraints(
            ordering = ResultOrdering(InstitutionGroup.SortBy.Name),
            filters = Some(InstitutionGroup.FilterBy(createdBefore = Some(date)))
          )

        val result =
          repository.getBy(institutionId, query).unsafeRunSync().groups
        result.size mustBe expectedCount
      }

      testDate(beforeDate, expectedCount = 0)
      testDate(afterDate, expectedCount = 2)
    }

    "sort results by name" in {
      val query: InstitutionGroup.PaginatedQuery =
        PaginatedQueryConstraints(ordering =
          ResultOrdering(
            InstitutionGroup.SortBy.Name,
            PaginatedQueryConstraints.ResultOrdering.Direction.Descending
          )
        )

      assertGetByResult(query, List("Group 3", "Group 2", "Group 1"))
    }

    "sort results by created date" in {
      val groups = List("Group 1", "Group 2").map(InstitutionGroup.Name.apply)
      val institutionId = createParticipant("Institution-1")
      createInstitutionGroup(institutionId, groups.head)
      // Sleep 1 ms to ensure DB queries sorting by creation time are deterministic (this only happens during testing)
      Thread.sleep(1)
      createInstitutionGroup(institutionId, groups(1))

      val query: InstitutionGroup.PaginatedQuery =
        PaginatedQueryConstraints(ordering =
          ResultOrdering(
            InstitutionGroup.SortBy.CreatedAt,
            PaginatedQueryConstraints.ResultOrdering.Direction.Descending
          )
        )

      val result = repository.getBy(institutionId, query).unsafeRunSync().groups
      result.map(_.value.name.value) must be(List("Group 2", "Group 1"))
    }

    "sort results by contact count" in {
      val groups = List("Group 1", "Group 2").map(InstitutionGroup.Name.apply)
      val institutionId = createParticipant("Institution-1")
      groups.foreach { g => createInstitutionGroup(institutionId, g) }
      createContact(institutionId, "test-contact-1", groups.headOption)
      createContact(institutionId, "test-contact-2", groups.headOption)
      createContact(institutionId, "test-contact-3", groups.lift(1))

      val query: InstitutionGroup.PaginatedQuery =
        PaginatedQueryConstraints(ordering = ResultOrdering(InstitutionGroup.SortBy.NumberOfContacts))

      val result = repository.getBy(institutionId, query).unsafeRunSync().groups
      result.map(_.value.name) must be(groups.reverse)
    }

    "respect limit" in {
      val limit = 2

      val query: InstitutionGroup.PaginatedQuery =
        PaginatedQueryConstraints(
          limit = limit,
          ordering = ResultOrdering(InstitutionGroup.SortBy.Name)
        )

      assertGetByResult(query, List("Group 1", "Group 2"))
    }

    "respect offset" in {
      val offset = 1

      val query: InstitutionGroup.PaginatedQuery =
        PaginatedQueryConstraints(
          offset = offset,
          ordering = ResultOrdering(InstitutionGroup.SortBy.Name)
        )

      assertGetByResult(query, List("Group 2", "Group 3"))
    }

    "return correct number of groups" in {
      val groups =
        List("Group 1", "Group 2", "Group 3").map(InstitutionGroup.Name.apply)
      val institutionId = createParticipant("Institution-1")
      groups.foreach { g => createInstitutionGroup(institutionId, g) }

      val query: InstitutionGroup.PaginatedQuery =
        PaginatedQueryConstraints(
          limit = 1,
          ordering = ResultOrdering(InstitutionGroup.SortBy.Name)
        )

      val result =
        repository.getBy(institutionId, query).unsafeRunSync()
      result.groups.size mustBe 1
      result.groups.head.value.name mustBe groups.head
      result.totalNumberOfRecords mustBe groups.size
    }

    def assertGetByResult(
        query: InstitutionGroup.PaginatedQuery,
        expectedResult: List[String]
    ) = {
      val groups =
        List("Group 1", "Group 2", "Group 3").map(InstitutionGroup.Name.apply)
      val institutionId = createParticipant("Institution-1")
      groups.foreach { g => createInstitutionGroup(institutionId, g) }

      val result = repository.getBy(institutionId, query).unsafeRunSync().groups
      result.map(_.value.name.value) must be(expectedResult)
    }
  }

  "copyGroup" should {
    "allow to copy group" in {
      val originalGroupName = InstitutionGroup.Name("IOHK 2019")
      val newGroupName = InstitutionGroup.Name("IOHK 2021")
      val institutionId = createParticipant("Institution-1")

      val maybeGroup = repository
        .create(institutionId, originalGroupName, Set())
        .unsafeToFuture()
        .futureValue
      maybeGroup.isRight mustBe true

      val originalGroupId = maybeGroup.toOption.get.id

      createContact(institutionId, "test-contact-1", originalGroupName.some)
      createContact(institutionId, "test-contact-2", originalGroupName.some)

      //To ensure that contacts are added
      repository
        .listContacts(institutionId, originalGroupName)
        .unsafeRunSync()
        .size mustBe 2

      val maybeNewGroup = repository
        .copyGroup(institutionId, originalGroupId, newGroupName)
        .unsafeRunSync()

      maybeNewGroup.isRight mustBe true

      val newGroup = maybeNewGroup.toOption.get
      newGroup.name mustBe newGroupName
      newGroup.institutionId mustBe institutionId

      val maybeOriginalContacts =
        Try(
          repository
            .listContacts(institutionId, originalGroupName)
            .unsafeToFuture()
            .futureValue
        )
      val maybeNewGroupContacts = repository
        .listContacts(institutionId, newGroupName)
        .unsafeToFuture()
        .futureValue

      maybeOriginalContacts.isSuccess mustBe true
      maybeOriginalContacts mustBe Success(maybeNewGroupContacts)
    }

    "return error if group doesn't exists" in {
      val institutionId = createParticipant("Institution-1")
      val fakeGroupId = InstitutionGroup.Id(UUID.randomUUID())
      val newGroupName = InstitutionGroup.Name("IOHK 2021")

      repository
        .copyGroup(institutionId, fakeGroupId, newGroupName)
        .unsafeRunSync() mustBe Left(
        GroupsInstitutionDoNotMatch(List(fakeGroupId), institutionId)
      )
    }

    "reject group copying if there is the wrong institutionId" in {
      val groupName = InstitutionGroup.Name("IOHK 2019")
      val newGroupName = InstitutionGroup.Name("IOHK 2021")
      val institutionId = createParticipant("Institution-1")
      val imposterInstitutionId = createParticipant("Institution-2")

      val maybeGroup = repository
        .create(institutionId, groupName, Set())
        .unsafeToFuture()
        .futureValue
      maybeGroup.isRight mustBe true

      val group = maybeGroup.toOption.get

      repository
        .copyGroup(imposterInstitutionId, group.id, newGroupName)
        .unsafeRunSync() mustBe Left(
        GroupsInstitutionDoNotMatch(List(group.id), imposterInstitutionId)
      )
    }

    "copy group even without contacts" in {
      val originalGroupName = InstitutionGroup.Name("IOHK 2019")
      val newGroupName = InstitutionGroup.Name("IOHK 2021")
      val institutionId = createParticipant("Institution-1")

      val maybeGroup = repository
        .create(institutionId, originalGroupName, Set())
        .unsafeToFuture()
        .futureValue
      maybeGroup.isRight mustBe true

      val originalGroupId = maybeGroup.toOption.get.id

      val maybeNewGroup = repository
        .copyGroup(institutionId, originalGroupId, newGroupName)
        .unsafeRunSync()

      maybeNewGroup.isRight mustBe true

      val newGroup = maybeNewGroup.toOption.get
      newGroup.name mustBe newGroupName
      newGroup.institutionId mustBe institutionId

      val maybeOriginalContacts = repository
        .listContacts(institutionId, originalGroupName)
        .unsafeToFuture()
        .futureValue
      val maybeNewGroupContacts = repository
        .listContacts(institutionId, newGroupName)
        .unsafeToFuture()
        .futureValue

      maybeOriginalContacts mustBe Nil
      maybeOriginalContacts mustBe maybeNewGroupContacts
    }

    "reject copying if the new name already taken by another group" in {
      val originalGroupName = InstitutionGroup.Name("IOHK 2019")
      val group2Name = InstitutionGroup.Name("IOHK 2021")
      val institutionId = createParticipant("Institution-1")

      val maybeGroup = repository
        .create(institutionId, originalGroupName, Set())
        .unsafeToFuture()
        .futureValue
      maybeGroup.isRight mustBe true
      repository
        .create(institutionId, group2Name, Set())
        .unsafeRunSync()
        .isRight mustBe true

      val originalGroupId = maybeGroup.toOption.get.id

      repository
        .copyGroup(institutionId, originalGroupId, group2Name)
        .unsafeRunSync() mustBe Left(GroupNameIsNotFree(group2Name))
    }
  }

  "deleteGroup" should {
    "allow to delete group" in {
      val groupName = InstitutionGroup.Name("IOHK 2019")
      val institutionId = createParticipant("Institution-1")

      val maybeGroup = repository
        .create(institutionId, groupName, Set())
        .unsafeToFuture()
        .futureValue
      maybeGroup.isRight mustBe true

      val groupId = maybeGroup.toOption.get.id

      createContact(institutionId, "test-contact-1", groupName.some)
      createContact(institutionId, "test-contact-2", groupName.some)

      //To ensure that contacts are added
      repository
        .listContacts(institutionId, groupName)
        .unsafeRunSync()
        .size mustBe 2

      repository
        .deleteGroup(institutionId, groupId)
        .unsafeToFuture()
        .futureValue
        .isRight mustBe true

      val query: InstitutionGroup.PaginatedQuery =
        PaginatedQueryConstraints(ordering = ResultOrdering(InstitutionGroup.SortBy.Name))

      repository.getBy(institutionId, query).unsafeRunSync().groups mustBe Nil
      //Guarantee that we removed contacts and group
      intercept[RuntimeException](
        repository
          .listContacts(institutionId, groupName)
          .unsafeToFuture()
          .futureValue
      )
      succeed
    }

    "return error if group doesn't exists" in {
      val institutionId = createParticipant("Institution-1")
      val fakeGroupId = InstitutionGroup.Id(UUID.randomUUID())

      repository
        .deleteGroup(institutionId, fakeGroupId)
        .unsafeRunSync() mustBe Left(
        GroupsInstitutionDoNotMatch(List(fakeGroupId), institutionId)
      )
    }

    "reject group deleting if there is wrong institutionId" in {
      val groupName = InstitutionGroup.Name("IOHK 2019")
      val institutionId = createParticipant("Institution-1")
      val imposterInstitutionId = createParticipant("Institution-2")

      val maybeGroup = repository
        .create(institutionId, groupName, Set())
        .unsafeToFuture()
        .futureValue
      maybeGroup.isRight mustBe true

      val group = maybeGroup.toOption.get

      repository
        .deleteGroup(imposterInstitutionId, group.id)
        .unsafeRunSync() mustBe Left(
        GroupsInstitutionDoNotMatch(List(group.id), imposterInstitutionId)
      )
    }
  }
}
