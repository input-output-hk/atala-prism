package io.iohk.atala.prism.management.console.repositories

import io.circe.Json
import io.iohk.atala.prism.AtalaWithPostgresSpec
import io.iohk.atala.prism.management.console.DataPreparation
import io.iohk.atala.prism.management.console.DataPreparation._
import io.iohk.atala.prism.management.console.errors.{ContactHasExistingCredentials, ContactsInstitutionsDoNotMatch}
import io.iohk.atala.prism.management.console.models._
import io.iohk.atala.prism.management.console.repositories.daos.InstitutionGroupsDAO
import org.scalatest.OptionValues._

import java.time.{Instant, LocalDate, Period}
import java.util.UUID

// sbt "project management-console" "testOnly *ContactsRepositorySpec"
class ContactsRepositorySpec extends AtalaWithPostgresSpec {
  import PaginatedQueryConstraints._

  lazy val repository = new ContactsRepository(database)
  lazy val credentialsRepository = new CredentialsRepository(database)

  "create" should {
    "create a new subject and assign it to an specified group" in {
      val institutionId = createParticipant("Institution-1")
      val group = createInstitutionGroup(institutionId, InstitutionGroup.Name("Grp 1"))
      val externalId = Contact.ExternalId.random()
      val json = Json.obj(
        "universityId" -> Json.fromString("uid"),
        "email" -> Json.fromString("d.here@iohk.io"),
        "admissionDate" -> Json.fromString(LocalDate.now().toString)
      )
      val request = CreateContact(institutionId, externalId, json, "Dusty Here")

      val result = repository.create(request, Some(group.name)).value.futureValue
      val subject = result.toOption.value
      subject.data must be(json)
      subject.externalId must be(externalId)

      // we check that the subject was added to the intended group
      val subjectsInGroupList = repository
        .getBy(institutionId, Helpers.legacyQuery(None, Some(group.name), 10))
        .value
        .futureValue
        .toOption
        .value
        .map(_.details)

      subjectsInGroupList.size must be(1)
      subjectsInGroupList.headOption.value must be(subject)
    }

    "create a new subject and assign it to no specified group" in {
      val institution = createParticipant("Institution-1")
      val externalId = Contact.ExternalId.random()
      val json = Json.obj(
        "universityId" -> Json.fromString("uid"),
        "email" -> Json.fromString("d.here@iohk.io"),
        "admissionDate" -> Json.fromString(LocalDate.now().toString)
      )
      val request = CreateContact(institution, externalId, json, "Dusty Here")

      val result = repository.create(request, None).value.futureValue
      val subject = result.toOption.value
      subject.data must be(json)
      subject.externalId must be(externalId)

      // we check that the subject was added
      val maybeSubject = repository.find(institution, subject.contactId).value.futureValue.toOption.value.value
      maybeSubject.contact must be(subject)
    }

    "fail to create a new subject when the specified group does not exist" in {
      val institutionId = createParticipant("Institution-1")
      val externalId = Contact.ExternalId.random()
      val json = Json.obj(
        "universityId" -> Json.fromString("uid"),
        "email" -> Json.fromString("d.here@iohk.io"),
        "admissionDate" -> Json.fromString(LocalDate.now().toString)
      )
      val request = CreateContact(institutionId, externalId, json, "Dusty Here")

      intercept[Exception](
        repository.create(request, Some(InstitutionGroup.Name("Grp 1"))).value.futureValue
      )

      // we check that the subject was not created
      val subjectsList = repository
        .getBy(institutionId, Helpers.legacyQuery(None, None, 1))
        .value
        .futureValue
        .toOption
        .value
        .map(_.details)
      subjectsList must be(empty)
    }

    "fail to create a new subject with empty external id" in {
      val institutionId = createParticipant("Institution-1")
      val group = createInstitutionGroup(institutionId, InstitutionGroup.Name("Grp 1"))
      val externalId = Contact.ExternalId("")
      val json = Json.obj(
        "universityId" -> Json.fromString("uid"),
        "email" -> Json.fromString("d.here@iohk.io"),
        "admissionDate" -> Json.fromString(LocalDate.now().toString)
      )
      val request = CreateContact(institutionId, externalId, json, "Dusty Here")

      intercept[Exception](
        repository.create(request, Some(group.name)).value.futureValue
      )
      // no subject should be created
      val createdSubjects = repository
        .getBy(institutionId, Helpers.legacyQuery(None, None, 10))
        .value
        .futureValue
        .toOption
        .value
        .map(_.details)
      createdSubjects must be(empty)
    }

    "fail to create a new subject with an external id already used" in {
      val institutionId = createParticipant("Institution-1")
      val group = createInstitutionGroup(institutionId, InstitutionGroup.Name("Grp 1"))
      val externalId = Contact.ExternalId.random()
      val json = Json.obj(
        "universityId" -> Json.fromString("uid"),
        "email" -> Json.fromString("d.here@iohk.io"),
        "admissionDate" -> Json.fromString(LocalDate.now().toString)
      )
      val request = CreateContact(institutionId, externalId, json, "Dusty Here")

      val initialResponse = repository.create(request, Some(group.name)).value.futureValue.toOption.value

      val secondJson = Json.obj(
        "universityId" -> Json.fromString("uid"),
        "email" -> Json.fromString("d.here@iohk.io"),
        "admissionDate" -> Json.fromString(LocalDate.now().toString)
      )

      val secondRequest = CreateContact(institutionId, externalId, secondJson, "Dusty Here")

      intercept[Exception](
        repository.create(secondRequest, Some(group.name)).value.futureValue
      )

      val subjectsStored = repository
        .getBy(institutionId, Helpers.legacyQuery(None, None, 10))
        .value
        .futureValue
        .toOption
        .value
        .map(_.details)

      // only one subject must be inserted correctly
      subjectsStored.size must be(1)

      val subject = subjectsStored.head
      // the subject must have the original data
      subject.data must be(json)
      subject.contactId must be(initialResponse.contactId)
      subject.externalId must be(externalId)
    }
  }

  "createBatch" should {
    "work when there are no contacts nor groups" in {
      val institutionId = createParticipant("Institution-1")
      val request = CreateContact.Batch(Set.empty, List.empty)

      val result = repository.createBatch(institutionId, request).value.futureValue
      result.isRight must be(true)
    }

    "create several contacts with no groups" in {
      val institutionId = createParticipant("Institution-1")
      val json = Json.obj(
        "universityId" -> Json.fromString("uid"),
        "email" -> Json.fromString("d.here@iohk.io"),
        "admissionDate" -> Json.fromString(LocalDate.now().toString)
      )
      val request = CreateContact.Batch(
        Set.empty,
        List(
          CreateContact.NoOwner(Contact.ExternalId.random(), json, "Dusty 1"),
          CreateContact.NoOwner(Contact.ExternalId.random(), json, "Dusty 2"),
          CreateContact.NoOwner(Contact.ExternalId.random(), json, "Dusty 3")
        )
      )
      val result = repository.createBatch(institutionId, request).value.futureValue
      result.isRight must be(true)

      // check that the contacts were created
      val stored = repository
        .getBy(institutionId, Helpers.legacyQuery())
        .value
        .futureValue
        .toOption
        .value

      stored.map(_.details.name).toSet must be(Set("Dusty 1", "Dusty 2", "Dusty 3"))
    }

    "create several contacts and assign them to several groups" in {
      val institutionId = createParticipant("Institution-1")
      val groups = List(
        createInstitutionGroup(institutionId, InstitutionGroup.Name("Group 1")),
        createInstitutionGroup(institutionId, InstitutionGroup.Name("Group 2"))
      )
      val json = Json.obj(
        "universityId" -> Json.fromString("uid"),
        "email" -> Json.fromString("d.here@iohk.io"),
        "admissionDate" -> Json.fromString(LocalDate.now().toString)
      )
      val request = CreateContact.Batch(
        groups.map(_.id).toSet,
        List(
          CreateContact.NoOwner(Contact.ExternalId.random(), json, "Dusty 1"),
          CreateContact.NoOwner(Contact.ExternalId.random(), json, "Dusty 2"),
          CreateContact.NoOwner(Contact.ExternalId.random(), json, "Dusty 3")
        )
      )
      val result = repository.createBatch(institutionId, request).value.futureValue
      result.isRight must be(true)

      // we check that the contact was added to the intended group
      groups.foreach { group =>
        listGroupContacts(group.id).map(_.name).toSet must be(Set("Dusty 1", "Dusty 2", "Dusty 3"))
      }
    }

    "fail to create contacts when there are duplicates by externalId in the batch" in {
      val institutionId = createParticipant("Institution-1")
      val externalId = Contact.ExternalId.random()
      val json = Json.obj(
        "universityId" -> Json.fromString("uid"),
        "email" -> Json.fromString("d.here@iohk.io"),
        "admissionDate" -> Json.fromString(LocalDate.now().toString)
      )
      val request = CreateContact.Batch(
        Set.empty,
        List(
          CreateContact.NoOwner(Contact.ExternalId.random(), json, "Dusty 1"),
          CreateContact.NoOwner(externalId, json, "Dusty 2"),
          CreateContact.NoOwner(externalId, json, "Dusty 3")
        )
      )

      intercept[RuntimeException] {
        repository.createBatch(institutionId, request).value.futureValue
      }

      // check that no contacts were created
      val stored = repository
        .getBy(institutionId, Helpers.legacyQuery())
        .value
        .futureValue
        .toOption
        .value

      stored must be(empty)
    }

    "fail to create contacts when there are duplicates by externalId in the database" in {
      val institutionId = createParticipant("Institution-1")
      val externalId = Contact.ExternalId.random()
      val json = Json.obj(
        "universityId" -> Json.fromString("uid"),
        "email" -> Json.fromString("d.here@iohk.io"),
        "admissionDate" -> Json.fromString(LocalDate.now().toString)
      )

      val result1 = repository
        .createBatch(
          institutionId,
          CreateContact.Batch(
            Set.empty,
            List(
              CreateContact.NoOwner(externalId, json, "Dusty 1")
            )
          )
        )
        .value
        .futureValue
      result1.isRight must be(true)

      val request = CreateContact.Batch(
        Set.empty,
        List(
          CreateContact.NoOwner(Contact.ExternalId.random(), json, "Dusty 2"),
          CreateContact.NoOwner(externalId, json, "Dusty 3")
        )
      )

      intercept[RuntimeException] {
        repository.createBatch(institutionId, request).value.futureValue
      }

      // check that no contacts were created
      val stored = repository
        .getBy(institutionId, Helpers.legacyQuery())
        .value
        .futureValue
        .toOption
        .value

      stored.map(_.details.name) must be(List("Dusty 1"))
    }

    "fail to create contacts when there is a group that doesn't belong to the institution" in {
      val institutionId = createParticipant("Institution-1")
      val institution2Id = createParticipant("Institution-2")
      val groups = List(
        createInstitutionGroup(institutionId, InstitutionGroup.Name("Group 1")),
        createInstitutionGroup(institution2Id, InstitutionGroup.Name("Group 2"))
      )
      val json = Json.obj(
        "universityId" -> Json.fromString("uid"),
        "email" -> Json.fromString("d.here@iohk.io"),
        "admissionDate" -> Json.fromString(LocalDate.now().toString)
      )
      val request = CreateContact.Batch(
        groups.map(_.id).toSet,
        List(
          CreateContact.NoOwner(Contact.ExternalId.random(), json, "Dusty 1"),
          CreateContact.NoOwner(Contact.ExternalId.random(), json, "Dusty 2"),
          CreateContact.NoOwner(Contact.ExternalId.random(), json, "Dusty 3")
        )
      )
      val result = repository.createBatch(institutionId, request).value.futureValue
      result.isLeft must be(true)
    }

    "fail to create contacts when there is a group that doesn't exist" in {
      val institutionId = createParticipant("Institution-1")
      val groups = List(
        createInstitutionGroup(institutionId, InstitutionGroup.Name("Group 1")),
        createInstitutionGroup(institutionId, InstitutionGroup.Name("Group 2"))
      )
      val json = Json.obj(
        "universityId" -> Json.fromString("uid"),
        "email" -> Json.fromString("d.here@iohk.io"),
        "admissionDate" -> Json.fromString(LocalDate.now().toString)
      )
      val request = CreateContact.Batch(
        groups.map(_.id).toSet[InstitutionGroup.Id] + InstitutionGroup.Id.random(),
        List(
          CreateContact.NoOwner(Contact.ExternalId.random(), json, "Dusty 1"),
          CreateContact.NoOwner(Contact.ExternalId.random(), json, "Dusty 2"),
          CreateContact.NoOwner(Contact.ExternalId.random(), json, "Dusty 3")
        )
      )
      val result = repository.createBatch(institutionId, request).value.futureValue
      result.isLeft must be(true)
    }
  }

  "updateContact" should {
    "work" in {
      val institution = createParticipant("Institution-1")
      val json = Json.obj(
        "email" -> Json.fromString("d.here@iohk.io"),
        "admissionDate" -> Json.fromString(LocalDate.now().toString)
      )

      val contactId = repository
        .create(CreateContact(institution, Contact.ExternalId.random(), json, "Dusty Here"), None)
        .value
        .futureValue
        .toOption
        .value
        .contactId

      val newData = Json.obj(
        "level" -> Json.fromString("expert"),
        "blockchain" -> Json.fromString("true")
      )
      val request = UpdateContact(
        id = contactId,
        newName = "new dusty",
        newExternalId = Contact.ExternalId.random(),
        newData = newData
      )

      val result = repository.updateContact(institution, request).value.futureValue
      result.isRight must be(true)

      // we check that the contact was updated
      val contactWithDetails = repository.find(institution, contactId).value.futureValue.toOption.value.value
      val storedContact = contactWithDetails.contact
      storedContact.name must be(request.newName)
      storedContact.externalId must be(request.newExternalId)
      storedContact.data must be(request.newData)
    }

    "fail when the contact doesn't exists" in {
      val institution = createParticipant("Institution-1")
      val json = Json.obj(
        "email" -> Json.fromString("d.here@iohk.io"),
        "admissionDate" -> Json.fromString(LocalDate.now().toString)
      )

      val request = UpdateContact(
        id = Contact.Id.random(),
        newName = "new dusty",
        newExternalId = Contact.ExternalId.random(),
        newData = json
      )

      intercept[RuntimeException] {
        repository.updateContact(institution, request).value.futureValue
      }
    }

    "fail when the contact doesn't belong to the given institution" in {
      val institution = createParticipant("Institution-1")
      val json = Json.obj(
        "email" -> Json.fromString("d.here@iohk.io"),
        "admissionDate" -> Json.fromString(LocalDate.now().toString)
      )

      val contactId = repository
        .create(CreateContact(institution, Contact.ExternalId.random(), json, "Dusty Here"), None)
        .value
        .futureValue
        .toOption
        .value
        .contactId

      val newData = Json.obj(
        "level" -> Json.fromString("expert"),
        "blockchain" -> Json.fromString("true")
      )
      val request = UpdateContact(
        id = contactId,
        newName = "new dusty",
        newExternalId = Contact.ExternalId.random(),
        newData = newData
      )

      intercept[RuntimeException] {
        repository.updateContact(ParticipantId.random(), request).value.futureValue
      }
    }
  }

  "find by contactId" should {
    "return the correct contact when present" in {
      val institutionId = createParticipant("Institution X")
      val group = createInstitutionGroup(institutionId, InstitutionGroup.Name("Group A"))
      val contactA = createContact(institutionId, "Alice", Some(group.name))
      createContact(institutionId, "Bob", Some(group.name))

      val contactWithDetails = repository.find(institutionId, contactA.contactId).value.futureValue.toOption.value.value
      contactWithDetails.contact must be(contactA)
    }

    "return the correct contact with groups involved" in {
      val institutionId = createParticipant("Institution X")
      val group = createInstitutionGroup(institutionId, InstitutionGroup.Name("Group A"))
      val contactA = createContact(institutionId, "Alice", Some(group.name))
      createContact(institutionId, "Bob", Some(group.name))

      val contactWithDetails = repository.find(institutionId, contactA.contactId).value.futureValue.toOption.value.value

      contactWithDetails.groupsInvolved.size mustBe 1
      contactWithDetails.groupsInvolved.head.value mustBe group
      contactWithDetails.groupsInvolved.head.numberOfContacts mustBe 2
    }

    "return the correct contact with issued credentials" in {
      val institutionId = createParticipant("Institution X")
      val contact = createContact(institutionId, "Alice", None)

      val credentialType = createCredentialType(institutionId, "sample")
      val issuedCredential = createGenericCredential(
        issuedBy = institutionId,
        subjectId = contact.contactId,
        tag = "tag1",
        credentialIssuanceContactId = None,
        credentialTypeId = Some(credentialType.credentialType.id)
      )
      publishCredential(institutionId, issuedCredential.credentialId)

      val contactWithDetails = repository.find(institutionId, contact.contactId).value.futureValue.toOption.value.value

      contactWithDetails.issuedCredentials.size mustBe 1
      contactWithDetails.issuedCredentials.head.copy(publicationData = None) mustBe issuedCredential
    }

    "return the correct contact with received credentials" in {
      val institutionId = createParticipant("Institution X")
      val contactA = createContact(institutionId, "Alice", None)
      createReceivedCredential(contactA.contactId)

      val contactWithDetails = repository.find(institutionId, contactA.contactId).value.futureValue.toOption.value.value

      contactWithDetails.receivedCredentials.size mustBe 1
    }

    "return no subject when the subject is missing (institutionId and subjectId not correlated)" in {
      val institutionXId = createParticipant("Institution X")
      val institutionYId = createParticipant("Institution Y")
      val groupNameA = createInstitutionGroup(institutionXId, InstitutionGroup.Name("Group A")).name
      val groupNameB = createInstitutionGroup(institutionYId, InstitutionGroup.Name("Group B")).name
      val subjectA = createContact(institutionXId, "Alice", Some(groupNameA))
      createContact(institutionYId, "Bob", Some(groupNameB))

      val result = repository.find(institutionYId, subjectA.contactId).value.futureValue.toOption.value
      result must be(empty)
    }
  }

  "find by externalId" should {
    "return the correct subject when present" in {
      val institutionId = createParticipant("Institution X")
      val subjectA = createContact(institutionId, "Alice", None)
      createContact(institutionId, "Bob", None)

      val result = repository.find(institutionId, subjectA.externalId).value.futureValue.toOption.value
      result.value must be(subjectA)
    }

    "return no subject when the subject is missing (institutionId and subjectId not correlated)" in {
      val institutionXId = createParticipant("Institution X")
      val institutionYId = createParticipant("Institution Y")
      val groupNameA = createInstitutionGroup(institutionXId, InstitutionGroup.Name("Group A")).name
      val groupNameB = createInstitutionGroup(institutionYId, InstitutionGroup.Name("Group B")).name
      val subjectA = createContact(institutionXId, "Alice", Some(groupNameA))
      createContact(institutionYId, "Bob", Some(groupNameB))

      val result = repository.find(institutionYId, subjectA.externalId).value.futureValue.toOption.value
      result must be(empty)
    }
  }

  "getBy" should {

    def testQuery(tag: String, sortBy: Contact.SortBy, desc: Boolean) = {
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
          case Contact.SortBy.Name => data.sortBy(_.name)
        }

        val sortedProperly = constraints.ordering.direction match {
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
        val contactA = createContact(institutionId, "Alice", Some(groupNameA))
        val contactB = createContact(institutionId, "Bob", Some(groupNameB))
        val contactC = createContact(institutionId, "Charles", Some(groupNameC))
        val contactD = createContact(institutionId, "Alice 2", Some(groupNameA))

        val expected = query(List(contactA, contactB, contactC, contactD), buildQuery(2))
        val result = repository
          .getBy(institutionId, buildQuery(2))
          .value
          .futureValue
          .toOption
          .value
          .map(_.details)

        result must be(expected)
      }

      s"[$tag] return the first contacts matching a group" in {
        val institutionId = createParticipant("Institution X")
        val groupNameA = createInstitutionGroup(institutionId, InstitutionGroup.Name("Group A")).name
        val groupNameB = createInstitutionGroup(institutionId, InstitutionGroup.Name("Group B")).name
        val groupNameC = createInstitutionGroup(institutionId, InstitutionGroup.Name("Group C")).name
        val contactA = createContact(institutionId, "Alice", Some(groupNameA))
        createContact(institutionId, "Bob", Some(groupNameB))
        createContact(institutionId, "Charles", Some(groupNameC))
        val contactD = createContact(institutionId, "Alice 2", Some(groupNameA))

        val expected = query(List(contactA, contactD), buildQuery(2, Some(groupNameA)))
        val result = repository
          .getBy(institutionId, buildQuery(2, Some(groupNameA)))
          .value
          .futureValue
          .toOption
          .value
          .map(_.details)

        result must be(expected)
      }

      s"[$tag] paginate by the last seen subject" in {
        val institutionId = createParticipant("Institution X")
        val groupNameA = createInstitutionGroup(institutionId, InstitutionGroup.Name("Group A")).name
        val groupNameB = createInstitutionGroup(institutionId, InstitutionGroup.Name("Group B")).name
        val groupNameC = createInstitutionGroup(institutionId, InstitutionGroup.Name("Group C")).name
        val contactA = createContact(institutionId, "Alice", Some(groupNameA))
        val contactB = createContact(institutionId, "Bob", Some(groupNameB))
        val contactC = createContact(institutionId, "Charles", Some(groupNameC))
        val contactD = createContact(institutionId, "Alice 2", Some(groupNameA))

        val scrollId = contactB.contactId
        val result = repository
          .getBy(institutionId, buildQuery(1, None, Some(scrollId)))
          .value
          .futureValue
          .toOption
          .value
          .map(_.details)

        val expected = query(List(contactA, contactB, contactC, contactD), buildQuery(1, scrollId = Some(scrollId)))
        result must be(expected)
      }

      s"[$tag] paginate by the last seen subject matching by group" in {
        val institutionId = createParticipant("Institution X")
        val groupNameA = createInstitutionGroup(institutionId, InstitutionGroup.Name("Group A")).name
        val groupNameB = createInstitutionGroup(institutionId, InstitutionGroup.Name("Group B")).name
        val groupNameC = createInstitutionGroup(institutionId, InstitutionGroup.Name("Group C")).name
        val contactA = createContact(institutionId, "Alice", Some(groupNameA))
        createContact(institutionId, "Bob", Some(groupNameB))
        createContact(institutionId, "Charles", Some(groupNameC))
        val contactD = createContact(institutionId, "Alice 2", Some(groupNameA))
        val scrollId = contactA.contactId

        val expected = query(List(contactA, contactD), buildQuery(1, scrollId = Some(scrollId)))
        val result = repository
          .getBy(institutionId, buildQuery(1, Some(groupNameA), Some(scrollId)))
          .value
          .futureValue
          .toOption
          .value
          .map(_.details)

        result must be(expected)
      }
    }

    List(
      ("sorted by createdAt asc", Contact.SortBy.createdAt, false),
      ("sorted by createdAt desc", Contact.SortBy.createdAt, true),
      ("sorted by externalId asc", Contact.SortBy.externalId, false),
      ("sorted by externalId desc", Contact.SortBy.externalId, true),
      ("sorted by name asc", Contact.SortBy.name, false),
      ("sorted by name desc", Contact.SortBy.name, true)
    ).foreach((testQuery _).tupled)

    def filterQuery(
        externalId: Option[String] = None,
        name: Option[String] = None,
        createdAt: Option[LocalDate] = None
    ): Contact.PaginatedQuery = {
      PaginatedQueryConstraints(
        limit = 10,
        ordering = ResultOrdering(Contact.SortBy.createdAt),
        filters = Some(
          Contact.FilterBy(
            externalId = externalId,
            name = name,
            createdAt = createdAt
          )
        )
      )
    }

    "return items matching the externalId" in {
      val institutionId = createParticipant("Institution X")
      val contactA = createContact(institutionId, "Alice", externalId = Contact.ExternalId("atala1"))
      val contactB = createContact(institutionId, "Bob", externalId = Contact.ExternalId("atala2"))
      createContact(institutionId, "Charles", externalId = Contact.ExternalId("atxala"))
      createContact(institutionId, "Alice 2", externalId = Contact.ExternalId("iohk2"))

      val expected = List(contactA, contactB).map(_.contactId).toSet
      val result = repository
        .getBy(institutionId, filterQuery(externalId = Some("tala")))
        .value
        .futureValue
        .toOption
        .value
        .map(_.details)

      result.map(_.contactId).toSet must be(expected)
    }

    "return items matching the contact name" in {
      val institutionId = createParticipant("Institution X")
      val contactA = createContact(institutionId, "Charles Hoskinson", externalId = Contact.ExternalId("atala1"))
      val contactB = createContact(institutionId, "Charles H", externalId = Contact.ExternalId("atala2"))
      createContact(institutionId, "Carlos", externalId = Contact.ExternalId("iohk1"))
      createContact(institutionId, "Alice 2", externalId = Contact.ExternalId("iohk2"))

      val expected = List(contactA, contactB).map(_.contactId).toSet
      val result = repository
        .getBy(institutionId, filterQuery(name = Some("harl")))
        .value
        .futureValue
        .toOption
        .value
        .map(_.details)

      result.map(_.contactId).toSet must be(expected)
    }

    "return items matching the createdAt" in {
      val institutionId = createParticipant("Institution X")
      val now = Instant.parse("2007-12-03T10:15:30.00Z")
      val contactA =
        createContact(institutionId, "Alice", externalId = Contact.ExternalId("atala1"), createdAt = Some(now))
      val contactB = createContact(
        institutionId,
        "Bob",
        externalId = Contact.ExternalId("atala2"),
        createdAt = Some(now.plusSeconds(10))
      )
      createContact(
        institutionId,
        "Charles",
        externalId = Contact.ExternalId("atxala"),
        createdAt = Some(now.plus(Period.ofDays(1)))
      )
      createContact(
        institutionId,
        "Alice 2",
        externalId = Contact.ExternalId("iohk2"),
        createdAt = Some(now.minus(Period.ofDays(1)))
      )

      val expected = List(contactA, contactB).map(_.contactId).toSet
      val result = repository
        .getBy(institutionId, filterQuery(createdAt = Some(LocalDate.parse("2007-12-03"))))
        .value
        .futureValue
        .toOption
        .value
        .map(_.details)

      result.map(_.contactId).toSet must be(expected)
    }

    "return items matching the externalId and contact name" in {
      val institutionId = createParticipant("Institution X")
      val contactA = createContact(institutionId, "Charles Hoskinson", externalId = Contact.ExternalId("atala1"))
      val contactB = createContact(institutionId, "Charles H", externalId = Contact.ExternalId("atala2"))
      createContact(institutionId, "Charles", externalId = Contact.ExternalId("iohk1"))
      createContact(institutionId, "Alice 2", externalId = Contact.ExternalId("atala"))

      val expected = List(contactA, contactB).map(_.contactId).toSet
      val result = repository
        .getBy(institutionId, filterQuery(name = Some("harl"), externalId = Some("tala")))
        .value
        .futureValue
        .toOption
        .value
        .map(_.details)

      result.map(_.contactId).toSet must be(expected)
    }

    "return items matching the createdAt and contact name" in {
      val institutionId = createParticipant("Institution X")
      val now = Instant.parse("2007-12-03T10:15:30.00Z")
      val contactA =
        createContact(institutionId, "iohk1", externalId = Contact.ExternalId("atala1"), createdAt = Some(now))
      val contactB =
        createContact(
          institutionId,
          "iohk2",
          externalId = Contact.ExternalId("atala2"),
          createdAt = Some(now.plusSeconds(10))
        )
      createContact(
        institutionId,
        "Charles",
        externalId = Contact.ExternalId("atxala"),
        createdAt = Some(now.plus(Period.ofDays(1)))
      )
      createContact(
        institutionId,
        "Alice 2",
        externalId = Contact.ExternalId("iohk2"),
        createdAt = Some(now.minus(Period.ofDays(1)))
      )

      val expected = List(contactA, contactB).map(_.contactId).toSet
      val result = repository
        .getBy(institutionId, filterQuery(name = Some("ioh"), createdAt = Some(LocalDate.parse("2007-12-03"))))
        .value
        .futureValue
        .toOption
        .value
        .map(_.details)

      result.map(_.contactId).toSet must be(expected)
    }

    "return items matching the createdAt and externalId" in {
      val institutionId = createParticipant("Institution X")
      val now = Instant.parse("2007-12-03T10:15:30.00Z")
      val contactA =
        createContact(institutionId, "iohk1", externalId = Contact.ExternalId("atala1"), createdAt = Some(now))
      val contactB =
        createContact(
          institutionId,
          "iohk2",
          externalId = Contact.ExternalId("atala2"),
          createdAt = Some(now.plusSeconds(10))
        )
      createContact(
        institutionId,
        "Charles",
        externalId = Contact.ExternalId("atala"),
        createdAt = Some(now.plus(Period.ofDays(1)))
      )
      createContact(
        institutionId,
        "iohkX",
        externalId = Contact.ExternalId("no"),
        createdAt = Some(now.minus(Period.ofDays(1)))
      )

      val expected = List(contactA, contactB).map(_.contactId).toSet
      val result = repository
        .getBy(
          institutionId,
          filterQuery(externalId = Some("atala"), createdAt = Some(LocalDate.parse("2007-12-03")))
        )
        .value
        .futureValue
        .toOption
        .value
        .map(_.details)

      result.map(_.contactId).toSet must be(expected)
    }

    "return items matching the createdAt, externalId, and contact name" in {
      val institutionId = createParticipant("Institution X")
      val now = Instant.parse("2007-12-03T10:15:30.00Z")
      val contactA =
        createContact(institutionId, "iohk1", externalId = Contact.ExternalId("atala1"), createdAt = Some(now))
      val contactB =
        createContact(
          institutionId,
          "iohk2",
          externalId = Contact.ExternalId("atala2"),
          createdAt = Some(now.plusSeconds(10))
        )
      createContact(
        institutionId,
        "Charles",
        externalId = Contact.ExternalId("atala"),
        createdAt = Some(now.plus(Period.ofDays(1)))
      )
      createContact(
        institutionId,
        "iohkX",
        externalId = Contact.ExternalId("no"),
        createdAt = Some(now.minus(Period.ofDays(1)))
      )

      val expected = List(contactA, contactB).map(_.contactId).toSet
      val result = repository
        .getBy(
          institutionId,
          filterQuery(externalId = Some("atala"), name = Some("iohk"), createdAt = Some(LocalDate.parse("2007-12-03")))
        )
        .value
        .futureValue
        .toOption
        .value
        .map(_.details)

      result.map(_.contactId).toSet must be(expected)
    }

    "return the credential counts" in {
      val institutionId = createParticipant("Institution X")
      val contactA = createContact(institutionId, "iohk1")
      val contactB = createContact(institutionId, "iohk2")

      DataPreparation.createGenericCredential(institutionId, contactA.contactId)
      DataPreparation.createGenericCredential(institutionId, contactA.contactId)
      DataPreparation.createGenericCredential(institutionId, contactA.contactId)
      DataPreparation.createReceivedCredential(contactA.contactId)
      DataPreparation.createReceivedCredential(contactA.contactId)
      DataPreparation.createGenericCredential(institutionId, contactB.contactId)

      val expected = Map((contactA.contactId, (3, 2)), (contactB.contactId, (1, 0)))
      val result = repository
        .getBy(institutionId, filterQuery())
        .value
        .futureValue
        .toOption
        .value

      result
        .map(r => (r.contactId, (r.counts.numberOfCredentialsCreated, r.counts.numberOfCredentialsReceived)))
        .toMap must be(expected)
    }
  }

  "delete" should {
    "delete the correct contact along with its credentials" in {
      val institutionId = createParticipant("Institution X")
      val contactA = createContact(institutionId, "iohk1")
      val contactB = createContact(institutionId, "iohk2")

      DataPreparation.createGenericCredential(institutionId, contactA.contactId)
      DataPreparation.createGenericCredential(institutionId, contactA.contactId)
      val contactBCredential1 = DataPreparation.createGenericCredential(institutionId, contactB.contactId)
      val contactBCredential2 = DataPreparation.createGenericCredential(institutionId, contactB.contactId)

      repository
        .delete(institutionId, contactA.contactId, deleteCredentials = true)
        .value
        .futureValue
        .toOption
        .value

      // Check that contact A was deleted
      repository
        .find(institutionId, contactA.contactId)
        .value
        .futureValue
        .toOption
        .flatten must be(None)

      // Check that contact B was not deleted
      repository
        .find(institutionId, contactB.contactId)
        .value
        .futureValue
        .toOption
        .flatten
        .map(_.contact) must be(Some(contactB))

      // Check that contact A credentials were deleted
      credentialsRepository
        .getBy(institutionId, contactA.contactId)
        .value
        .futureValue
        .toOption
        .value must be(List.empty)

      // Check that contact B credentials were not deleted
      credentialsRepository
        .getBy(institutionId, contactB.contactId)
        .value
        .futureValue
        .toOption
        .value must be(List(contactBCredential1, contactBCredential2))
    }

    "delete the correct contact along without deleting its credentials" in {
      val institutionId = createParticipant("Institution X")
      val contactA = createContact(institutionId, "iohk1")
      val contactB = createContact(institutionId, "iohk2")

      val contactBCredential1 = DataPreparation.createGenericCredential(institutionId, contactB.contactId)
      val contactBCredential2 = DataPreparation.createGenericCredential(institutionId, contactB.contactId)

      repository
        .delete(institutionId, contactA.contactId, deleteCredentials = false)
        .value
        .futureValue
        .toOption
        .value

      // Check that contact A was deleted
      repository
        .find(institutionId, contactA.contactId)
        .value
        .futureValue
        .toOption
        .flatten must be(None)

      // Check that contact B was not deleted
      repository
        .find(institutionId, contactB.contactId)
        .value
        .futureValue
        .toOption
        .flatten
        .map(_.contact) must be(Some(contactB))

      // Check that contact B credentials were not deleted
      credentialsRepository
        .getBy(institutionId, contactB.contactId)
        .value
        .futureValue
        .toOption
        .value must be(List(contactBCredential1, contactBCredential2))
    }

    "fail to delete contact without deleting its existing credentials" in {
      val institutionId = createParticipant("Institution X")
      val contact = createContact(institutionId, "iohk")

      DataPreparation.createGenericCredential(institutionId, contact.contactId)
      DataPreparation.createGenericCredential(institutionId, contact.contactId)

      val result = repository
        .delete(institutionId, contact.contactId, deleteCredentials = false)
        .value
        .futureValue

      result must be(Left(ContactHasExistingCredentials(contact.contactId)))
    }

    "fail to delete contact belonging to a different institution" in {
      val institutionId1 = createParticipant("Institution X")
      val institutionId2 = createParticipant("Institution Y")
      val contact = createContact(institutionId1, "iohk")

      DataPreparation.createGenericCredential(institutionId1, contact.contactId)
      DataPreparation.createGenericCredential(institutionId1, contact.contactId)

      val result = repository
        .delete(institutionId2, contact.contactId, deleteCredentials = false)
        .value
        .futureValue

      result must be(Left(ContactsInstitutionsDoNotMatch(List(contact.contactId), institutionId2)))
    }

    "fail to delete a non-existing contact" in {
      val institutionId = createParticipant("Institution X")
      val contactId = Contact.Id(UUID.randomUUID())

      val result = repository
        .delete(institutionId, contactId, deleteCredentials = false)
        .value
        .futureValue

      result must be(Left(ContactsInstitutionsDoNotMatch(List(contactId), institutionId)))
    }
  }

  private def listGroupContacts(groupId: InstitutionGroup.Id) = {

    import doobie.implicits._

    InstitutionGroupsDAO
      .listContacts(groupId)
      .transact(database)
      .unsafeRunSync()
  }
}
