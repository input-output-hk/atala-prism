package io.iohk.atala.prism.management.console.repositories

import io.circe.Json
import io.iohk.atala.prism.AtalaWithPostgresSpec
import io.iohk.atala.prism.management.console.DataPreparation._
import io.iohk.atala.prism.management.console.models.{
  Contact,
  CreateContact,
  InstitutionGroup,
  PaginatedQueryConstraints
}
import org.scalatest.OptionValues._

import java.time.LocalDate

class ContactsRepositorySpec extends AtalaWithPostgresSpec {
  lazy val repository = new ContactsRepository(database)

  "create" should {
    "create a new subject and assign it to an specified group" in {
      val institutionId = createParticipant("Institution-1")
      val group = createInstitutionGroup(institutionId, InstitutionGroup.Name("Grp 1"))
      val externalId = Contact.ExternalId.random()
      val json = Json.obj(
        "universityId" -> Json.fromString("uid"),
        "name" -> Json.fromString("Dusty Here"),
        "email" -> Json.fromString("d.here@iohk.io"),
        "admissionDate" -> Json.fromString(LocalDate.now().toString)
      )
      val request = CreateContact(institutionId, externalId, json)

      val result = repository.create(request, Some(group.name)).value.futureValue
      val subject = result.toOption.value
      subject.data must be(json)
      subject.externalId must be(externalId)

      // we check that the subject was added to the intended group
      val subjectsInGroupList = repository
        .getBy(institutionId, Contact.legacyQuery(None, Some(group.name), 10))
        .value
        .futureValue
        .toOption
        .value

      subjectsInGroupList.size must be(1)
      subjectsInGroupList.headOption.value must be(subject)
    }

    "create a new subject and assign it to no specified group" in {
      val institution = createParticipant("Institution-1")
      val externalId = Contact.ExternalId.random()
      val json = Json.obj(
        "universityId" -> Json.fromString("uid"),
        "name" -> Json.fromString("Dusty Here"),
        "email" -> Json.fromString("d.here@iohk.io"),
        "admissionDate" -> Json.fromString(LocalDate.now().toString)
      )
      val request = CreateContact(institution, externalId, json)

      val result = repository.create(request, None).value.futureValue
      val subject = result.toOption.value
      subject.data must be(json)
      subject.externalId must be(externalId)

      // we check that the subject was added
      val maybeSubject = repository.find(institution, subject.contactId).value.futureValue.toOption.value.value
      maybeSubject must be(subject)
    }

    "fail to create a new subject when the specified group does not exist" in {
      val institutionId = createParticipant("Institution-1")
      val externalId = Contact.ExternalId.random()
      val json = Json.obj(
        "universityId" -> Json.fromString("uid"),
        "name" -> Json.fromString("Dusty Here"),
        "email" -> Json.fromString("d.here@iohk.io"),
        "admissionDate" -> Json.fromString(LocalDate.now().toString)
      )
      val request = CreateContact(institutionId, externalId, json)

      intercept[Exception](
        repository.create(request, Some(InstitutionGroup.Name("Grp 1"))).value.futureValue
      )

      // we check that the subject was not created
      val subjectsList = repository
        .getBy(institutionId, Contact.legacyQuery(None, None, 1))
        .value
        .futureValue
        .toOption
        .value
      subjectsList must be(empty)
    }

    "fail to create a new subject with empty external id" in {
      val institutionId = createParticipant("Institution-1")
      val group = createInstitutionGroup(institutionId, InstitutionGroup.Name("Grp 1"))
      val externalId = Contact.ExternalId("")
      val json = Json.obj(
        "universityId" -> Json.fromString("uid"),
        "name" -> Json.fromString("Dusty Here"),
        "email" -> Json.fromString("d.here@iohk.io"),
        "admissionDate" -> Json.fromString(LocalDate.now().toString)
      )
      val request = CreateContact(institutionId, externalId, json)

      intercept[Exception](
        repository.create(request, Some(group.name)).value.futureValue
      )
      // no subject should be created
      val createdSubjects = repository
        .getBy(institutionId, Contact.legacyQuery(None, None, 10))
        .value
        .futureValue
        .toOption
        .value
      createdSubjects must be(empty)
    }

    "fail to create a new subject with an external id already used" in {
      val institutionId = createParticipant("Institution-1")
      val group = createInstitutionGroup(institutionId, InstitutionGroup.Name("Grp 1"))
      val externalId = Contact.ExternalId.random()
      val json = Json.obj(
        "universityId" -> Json.fromString("uid"),
        "name" -> Json.fromString("Dusty Here"),
        "email" -> Json.fromString("d.here@iohk.io"),
        "admissionDate" -> Json.fromString(LocalDate.now().toString)
      )
      val request = CreateContact(institutionId, externalId, json)

      val initialResponse = repository.create(request, Some(group.name)).value.futureValue.toOption.value

      val secondJson = Json.obj(
        "universityId" -> Json.fromString("uid"),
        "name" -> Json.fromString("Dusty Here"),
        "email" -> Json.fromString("d.here@iohk.io"),
        "admissionDate" -> Json.fromString(LocalDate.now().toString)
      )

      val secondRequest = CreateContact(institutionId, externalId, secondJson)

      intercept[Exception](
        repository.create(secondRequest, Some(group.name)).value.futureValue
      )

      val subjectsStored = repository
        .getBy(institutionId, Contact.legacyQuery(None, None, 10))
        .value
        .futureValue
        .toOption
        .value

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
      val institutionId = createParticipant("Institution X")
      val groupName = createInstitutionGroup(institutionId, InstitutionGroup.Name("Group A")).name
      val subjectA = createContact(institutionId, "Alice", groupName)
      createContact(institutionId, "Bob", groupName)

      val result = repository.find(institutionId, subjectA.contactId).value.futureValue.toOption.value
      result.value must be(subjectA)
    }

    "return no subject when the subject is missing (institutionId and subjectId not correlated)" in {
      val institutionXId = createParticipant("Institution X")
      val institutionYId = createParticipant("Institution Y")
      val groupNameA = createInstitutionGroup(institutionXId, InstitutionGroup.Name("Group A")).name
      val groupNameB = createInstitutionGroup(institutionYId, InstitutionGroup.Name("Group B")).name
      val subjectA = createContact(institutionXId, "Alice", groupNameA)
      createContact(institutionYId, "Bob", groupNameB)

      val result = repository.find(institutionYId, subjectA.contactId).value.futureValue.toOption.value
      result must be(empty)
    }
  }

  "find by externalId" should {
    "return the correct subject when present" in {
      val institutionId = createParticipant("Institution X")
      val subjectA = createContact(institutionId, "Alice", None, "subject-1")
      createContact(institutionId, "Bob", None, "subject-2")

      val result = repository.find(institutionId, subjectA.externalId).value.futureValue.toOption.value
      result.value must be(subjectA)
    }

    "return no subject when the subject is missing (institutionId and subjectId not correlated)" in {
      val institutionXId = createParticipant("Institution X")
      val institutionYId = createParticipant("Institution Y")
      val groupNameA = createInstitutionGroup(institutionXId, InstitutionGroup.Name("Group A")).name
      val groupNameB = createInstitutionGroup(institutionYId, InstitutionGroup.Name("Group B")).name
      val subjectA = createContact(institutionXId, "Alice", groupNameA)
      createContact(institutionYId, "Bob", groupNameB)

      val result = repository.find(institutionYId, subjectA.externalId).value.futureValue.toOption.value
      result must be(empty)
    }
  }

  "getBy" should {

    def testQuery(tag: String, sortBy: Contact.SortBy, desc: Boolean) = {
      import PaginatedQueryConstraints._

      def buildQuery(
          limit: Int,
          groupName: Option[InstitutionGroup.Name] = None,
          scrollId: Option[Contact.Id] = None
      ) = {
        val condition = if (desc) ResultOrdering.Direction.Descending else ResultOrdering.Direction.Ascending

        PaginatedQueryConstraints(
          limit = limit,
          ordering = ResultOrdering(sortBy, condition),
          scrollId = scrollId,
          filters = Some(Contact.FilterBy(groupName))
        )
      }

      // queries the in-memory data to verify results
      // NOTE: this doesn't filter by groups
      def query(data: List[Contact], constraints: Contact.PaginatedQuery): List[Contact] = {
        val sorted = constraints.ordering.field match {
          case Contact.SortBy.ExternalId => data.sortBy(_.externalId.value)
          case Contact.SortBy.CreatedAt => data.sortBy(_.createdAt)
        }

        val sortedProperly = constraints.ordering.condition match {
          case ResultOrdering.Direction.Ascending => sorted
          case ResultOrdering.Direction.Descending => sorted.reverse
        }

        val paginated = constraints.scrollId match {
          case Some(scrollId) => sortedProperly.dropWhile(_.contactId != scrollId).drop(1)
          case None => sortedProperly
        }

        paginated.take(constraints.limit)
      }

      s"[$tag] return the first contacts" in {
        val institutionId = createParticipant("Institution X")
        val groupNameA = createInstitutionGroup(institutionId, InstitutionGroup.Name("Group A")).name
        val groupNameB = createInstitutionGroup(institutionId, InstitutionGroup.Name("Group B")).name
        val groupNameC = createInstitutionGroup(institutionId, InstitutionGroup.Name("Group C")).name
        val contactA = createContact(institutionId, "Alice", groupNameA)
        val contactB = createContact(institutionId, "Bob", groupNameB)
        val contactC = createContact(institutionId, "Charles", groupNameC)
        val contactD = createContact(institutionId, "Alice 2", groupNameA)

        val expected = query(List(contactA, contactB, contactC, contactD), buildQuery(2))
        val result = repository
          .getBy(institutionId, buildQuery(2))
          .value
          .futureValue
          .toOption
          .value

        result must be(expected)
      }

      s"[$tag] return the first contacts matching a group" in {
        val institutionId = createParticipant("Institution X")
        val groupNameA = createInstitutionGroup(institutionId, InstitutionGroup.Name("Group A")).name
        val groupNameB = createInstitutionGroup(institutionId, InstitutionGroup.Name("Group B")).name
        val groupNameC = createInstitutionGroup(institutionId, InstitutionGroup.Name("Group C")).name
        val contactA = createContact(institutionId, "Alice", groupNameA)
        createContact(institutionId, "Bob", groupNameB)
        createContact(institutionId, "Charles", groupNameC)
        val contactD = createContact(institutionId, "Alice 2", groupNameA)

        val expected = query(List(contactA, contactD), buildQuery(2, Some(groupNameA)))
        val result = repository
          .getBy(institutionId, buildQuery(2, Some(groupNameA)))
          .value
          .futureValue
          .toOption
          .value

        result must be(expected)
      }

      s"[$tag] paginate by the last seen subject" in {
        val institutionId = createParticipant("Institution X")
        val groupNameA = createInstitutionGroup(institutionId, InstitutionGroup.Name("Group A")).name
        val groupNameB = createInstitutionGroup(institutionId, InstitutionGroup.Name("Group B")).name
        val groupNameC = createInstitutionGroup(institutionId, InstitutionGroup.Name("Group C")).name
        val contactA = createContact(institutionId, "Alice", groupNameA)
        val contactB = createContact(institutionId, "Bob", groupNameB)
        val contactC = createContact(institutionId, "Charles", groupNameC)
        val contactD = createContact(institutionId, "Alice 2", groupNameA)

        val scrollId = contactB.contactId
        val result = repository
          .getBy(institutionId, buildQuery(1, None, Some(scrollId)))
          .value
          .futureValue
          .toOption
          .value

        val expected = query(List(contactA, contactB, contactC, contactD), buildQuery(1, scrollId = Some(scrollId)))
        result must be(expected)
      }

      s"[$tag] paginate by the last seen subject matching by group" in {
        val institutionId = createParticipant("Institution X")
        val groupNameA = createInstitutionGroup(institutionId, InstitutionGroup.Name("Group A")).name
        val groupNameB = createInstitutionGroup(institutionId, InstitutionGroup.Name("Group B")).name
        val groupNameC = createInstitutionGroup(institutionId, InstitutionGroup.Name("Group C")).name
        val contactA = createContact(institutionId, "Alice", groupNameA)
        createContact(institutionId, "Bob", groupNameB)
        createContact(institutionId, "Charles", groupNameC)
        val contactD = createContact(institutionId, "Alice 2", groupNameA)
        val scrollId = contactA.contactId

        val expected = query(List(contactA, contactD), buildQuery(1, scrollId = Some(scrollId)))
        val result = repository
          .getBy(institutionId, buildQuery(1, Some(groupNameA), Some(scrollId)))
          .value
          .futureValue
          .toOption
          .value

        result must be(expected)
      }
    }

    List(
      ("sorted by createdAt asc", Contact.SortBy.createdAt, false),
      ("sorted by createdAt desc", Contact.SortBy.createdAt, true),
      ("sorted by externalId asc", Contact.SortBy.createdAt, false),
      ("sorted by externalId desc", Contact.SortBy.createdAt, true)
    ).foreach((testQuery _).tupled)
  }
}
