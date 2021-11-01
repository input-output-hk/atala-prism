package io.iohk.atala.prism.management.console.repositories

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import io.circe.Json
import io.iohk.atala.prism.AtalaWithPostgresSpec
import io.iohk.atala.prism.management.console.DataPreparation
import io.iohk.atala.prism.management.console.DataPreparation._
import io.iohk.atala.prism.management.console.errors.{ContactHasExistingCredentials, ContactsInstitutionsDoNotMatch}
import io.iohk.atala.prism.management.console.models._
import io.iohk.atala.prism.management.console.repositories.daos.InstitutionGroupsDAO
import io.iohk.atala.prism.models.ConnectionToken
import io.iohk.atala.prism.utils.IOUtils._
import org.scalatest.OptionValues._
import tofu.logging.Logs

import java.time.{Instant, LocalDate, Period}
import java.util.UUID
import scala.util.Try

// sbt "project management-console" "testOnly *ContactsRepositorySpec"
class ContactsRepositorySpec extends AtalaWithPostgresSpec {
  import PaginatedQueryConstraints._

  val logs: Logs[IO, IO] = Logs.sync[IO, IO]
  private val repository = ContactsRepository.unsafe(database, logs)
  val credentialsRepository = CredentialsRepository.unsafe(database, logs)

  "create" should {
    "create a new contact and assign it to an specified group" in {
      val institutionId = createParticipant("Institution-1")
      val group =
        createInstitutionGroup(institutionId, InstitutionGroup.Name("Grp 1"))
      val externalId = Contact.ExternalId.random()
      val json = Json.obj(
        "universityId" -> Json.fromString("uid"),
        "email" -> Json.fromString("d.here@iohk.io"),
        "admissionDate" -> Json.fromString(LocalDate.now().toString)
      )
      val request = CreateContact(
        externalId,
        json,
        "Dusty Here",
        grpcAuthenticationHeaderDIDBased
      )

      val result = repository
        .create(
          institutionId,
          request,
          Some(group.name),
          connectionToken = ConnectionToken("connectionToken")
        )
        .unsafeRunSync()
      val contact = result
      contact.data must be(json)
      contact.externalId must be(externalId)

      // we check that the contact was added to the intended group
      val contactsInGroupList = repository
        .getBy(institutionId, Helpers.legacyQuery(None, Some(group.name), 10))
        .unsafeRunSync()
        .map(_.details)

      contactsInGroupList.size must be(1)
      contactsInGroupList.headOption.value must be(contact)
    }

    "create a new contact and assign it to no specified group" in {
      val institution = createParticipant("Institution-1")
      val externalId = Contact.ExternalId.random()
      val json = Json.obj(
        "universityId" -> Json.fromString("uid"),
        "email" -> Json.fromString("d.here@iohk.io"),
        "admissionDate" -> Json.fromString(LocalDate.now().toString)
      )
      val request = CreateContact(
        externalId,
        json,
        "Dusty Here",
        grpcAuthenticationHeaderDIDBased
      )

      val result = repository
        .create(
          institution,
          request,
          None,
          connectionToken = ConnectionToken("connectionToken")
        )
        .unsafeRunSync()
      val contact = result
      contact.data must be(json)
      contact.externalId must be(externalId)

      // we check that the contact was added
      val maybeContact =
        repository.find(institution, contact.contactId).unsafeRunSync().value
      maybeContact.contact must be(contact)
    }

    "fail to create a new contact when the specified group does not exist" in {
      val institutionId = createParticipant("Institution-1")
      val externalId = Contact.ExternalId.random()
      val json = Json.obj(
        "universityId" -> Json.fromString("uid"),
        "email" -> Json.fromString("d.here@iohk.io"),
        "admissionDate" -> Json.fromString(LocalDate.now().toString)
      )
      val request = CreateContact(
        externalId,
        json,
        "Dusty Here",
        grpcAuthenticationHeaderDIDBased
      )

      intercept[Exception](
        repository
          .create(
            participantId = institutionId,
            contactData = request,
            maybeGroupName = Some(InstitutionGroup.Name("Grp 1")),
            connectionToken = ConnectionToken("connectionToken")
          )
          .unsafeRunSync()
      )

      // we check that the contact was not created
      val contactsList = repository
        .getBy(institutionId, Helpers.legacyQuery(None, None, 1))
        .unsafeRunSync()
        .map(_.details)
      contactsList must be(empty)
    }

    "fail to create a new contact with empty external id" in {
      val institutionId = createParticipant("Institution-1")
      val group =
        createInstitutionGroup(institutionId, InstitutionGroup.Name("Grp 1"))
      val externalId = Contact.ExternalId("")
      val json = Json.obj(
        "universityId" -> Json.fromString("uid"),
        "email" -> Json.fromString("d.here@iohk.io"),
        "admissionDate" -> Json.fromString(LocalDate.now().toString)
      )
      val request = CreateContact(
        externalId,
        json,
        "Dusty Here",
        grpcAuthenticationHeaderDIDBased
      )

      intercept[Exception](
        repository
          .create(
            institutionId,
            request,
            Some(group.name),
            connectionToken = ConnectionToken("connectionToken")
          )
          .unsafeRunSync()
      )
      // no contact should be created
      val createdContacts = repository
        .getBy(institutionId, Helpers.legacyQuery(None, None, 10))
        .unsafeRunSync()
        .map(_.details)
      createdContacts must be(empty)
    }

    "fail to create a new contact with an external id already used" in {
      val institutionId = createParticipant("Institution-1")
      val group =
        createInstitutionGroup(institutionId, InstitutionGroup.Name("Grp 1"))
      val externalId = Contact.ExternalId.random()
      val json = Json.obj(
        "universityId" -> Json.fromString("uid"),
        "email" -> Json.fromString("d.here@iohk.io"),
        "admissionDate" -> Json.fromString(LocalDate.now().toString)
      )
      val request = CreateContact(
        externalId,
        json,
        "Dusty Here",
        grpcAuthenticationHeaderDIDBased
      )

      val initialResponse = repository
        .create(
          institutionId,
          request,
          Some(group.name),
          connectionToken = ConnectionToken("connectionToken")
        )
        .unsafeRunSync()

      val secondJson = Json.obj(
        "universityId" -> Json.fromString("uid"),
        "email" -> Json.fromString("d.here@iohk.io"),
        "admissionDate" -> Json.fromString(LocalDate.now().toString)
      )

      val secondRequest = CreateContact(
        externalId,
        secondJson,
        "Dusty Here",
        grpcAuthenticationHeaderDIDBased
      )

      intercept[Exception](
        repository
          .create(
            institutionId,
            secondRequest,
            Some(group.name),
            connectionToken = ConnectionToken("connectionToken")
          )
          .unsafeRunSync()
      )

      val contactsStored = repository
        .getBy(institutionId, Helpers.legacyQuery(None, None, 10))
        .unsafeRunSync()
        .map(_.details)

      // only one contact must be inserted correctly
      contactsStored.size must be(1)

      val contact = contactsStored.head
      // the contact must have the original data
      contact.data must be(json)
      contact.contactId must be(initialResponse.contactId)
      contact.externalId must be(externalId)
    }
  }

  "createBatch" should {
    "work when there are no contacts nor groups" in {
      val institutionId = createParticipant("Institution-1")
      val request = CreateContact.Batch(
        Set.empty,
        List.empty,
        grpcAuthenticationHeaderDIDBased
      )

      val result = repository
        .createBatch(institutionId, request, List.empty)
        .unsafeRunSync()
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
        ),
        grpcAuthenticationHeaderDIDBased
      )
      val result =
        repository
          .createBatch(institutionId, request, makeConnectionTokens(count = 3))
          .unsafeToFuture()
          .futureValue
      result.isRight must be(true)

      // check that the contacts were created
      val stored = repository
        .getBy(institutionId, Helpers.legacyQuery())
        .unsafeToFuture()
        .futureValue

      stored.map(_.details.name).toSet must be(
        Set("Dusty 1", "Dusty 2", "Dusty 3")
      )
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
        ),
        grpcAuthenticationHeaderDIDBased
      )
      val result =
        repository
          .createBatch(institutionId, request, makeConnectionTokens(count = 3))
          .unsafeToFuture()
          .futureValue
      result.isRight must be(true)

      // we check that the contact was added to the intended group
      groups.foreach { group =>
        listGroupContacts(group.id).map(_.name).toSet must be(
          Set("Dusty 1", "Dusty 2", "Dusty 3")
        )
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
        ),
        grpcAuthenticationHeaderDIDBased
      )
      intercept[RuntimeException] {
        repository
          .createBatch(institutionId, request, makeConnectionTokens(count = 3))
          .unsafeToFuture()
          .futureValue
      }

      // check that no contacts were created
      val stored = repository
        .getBy(institutionId, Helpers.legacyQuery())
        .unsafeToFuture()
        .futureValue

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
            ),
            grpcAuthenticationHeaderDIDBased
          ),
          List(ConnectionToken("connectionToken"))
        )
        .unsafeToFuture()
        .futureValue
      result1.isRight must be(true)

      val request = CreateContact.Batch(
        Set.empty,
        List(
          CreateContact.NoOwner(Contact.ExternalId.random(), json, "Dusty 2"),
          CreateContact.NoOwner(externalId, json, "Dusty 3")
        ),
        grpcAuthenticationHeaderDIDBased
      )

      intercept[RuntimeException] {
        repository
          .createBatch(institutionId, request, makeConnectionTokens(count = 2))
          .unsafeToFuture()
          .futureValue
      }

      // check that no contacts were created
      val stored = repository
        .getBy(institutionId, Helpers.legacyQuery())
        .unsafeToFuture()
        .futureValue

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
        ),
        grpcAuthenticationHeaderDIDBased
      )
      val result =
        repository
          .createBatch(institutionId, request, makeConnectionTokens(count = 3))
          .unsafeToFuture()
          .futureValue
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
        groups.map(_.id).toSet[InstitutionGroup.Id] + InstitutionGroup.Id
          .random(),
        List(
          CreateContact.NoOwner(Contact.ExternalId.random(), json, "Dusty 1"),
          CreateContact.NoOwner(Contact.ExternalId.random(), json, "Dusty 2"),
          CreateContact.NoOwner(Contact.ExternalId.random(), json, "Dusty 3")
        ),
        grpcAuthenticationHeaderDIDBased
      )
      val result =
        repository
          .createBatch(institutionId, request, makeConnectionTokens(count = 3))
          .unsafeToFuture()
          .futureValue
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
        .create(
          participantId = institution,
          contactData = CreateContact(
            Contact.ExternalId.random(),
            json,
            "Dusty Here",
            grpcAuthenticationHeaderDIDBased
          ),
          maybeGroupName = None,
          connectionToken = ConnectionToken("connectionToken")
        )
        .unsafeToFuture()
        .futureValue
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

      val result =
        Try(
          repository
            .updateContact(institution, request)
            .unsafeToFuture()
            .futureValue
        ).toEither
      result.isRight must be(true)

      // we check that the contact was updated
      val contactWithDetails =
        repository
          .find(institution, contactId)
          .unsafeToFuture()
          .futureValue
          .value
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
        repository
          .updateContact(institution, request)
          .unsafeToFuture()
          .futureValue
      }
    }

    "fail when the contact doesn't belong to the given institution" in {
      val institution = createParticipant("Institution-1")
      val json = Json.obj(
        "email" -> Json.fromString("d.here@iohk.io"),
        "admissionDate" -> Json.fromString(LocalDate.now().toString)
      )

      val contactId = repository
        .create(
          participantId = institution,
          contactData = CreateContact(
            Contact.ExternalId.random(),
            json,
            "Dusty Here",
            grpcAuthenticationHeaderDIDBased
          ),
          maybeGroupName = None,
          connectionToken = ConnectionToken("connectionToken")
        )
        .unsafeToFuture()
        .futureValue
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
        repository
          .updateContact(ParticipantId.random(), request)
          .unsafeToFuture()
          .futureValue
      }
    }
  }

  "find by contactId" should {
    "return the correct contact when present" in {
      val institutionId = createParticipant("Institution X")
      val group =
        createInstitutionGroup(institutionId, InstitutionGroup.Name("Group A"))
      val contactA = createContact(institutionId, "Alice", Some(group.name))
      createContact(institutionId, "Bob", Some(group.name))

      val contactWithDetails =
        repository
          .find(institutionId, contactA.contactId)
          .unsafeToFuture()
          .futureValue
          .value
      contactWithDetails.contact must be(contactA)
    }

    "return the correct contact with groups involved" in {
      val institutionId = createParticipant("Institution X")
      val group =
        createInstitutionGroup(institutionId, InstitutionGroup.Name("Group A"))
      val contactA = createContact(institutionId, "Alice", Some(group.name))
      createContact(institutionId, "Bob", Some(group.name))

      val contactWithDetails =
        repository
          .find(institutionId, contactA.contactId)
          .unsafeToFuture()
          .futureValue
          .value

      contactWithDetails.groupsInvolved.size mustBe 1
      contactWithDetails.groupsInvolved.head.value mustBe group
      contactWithDetails.groupsInvolved.head.numberOfContacts mustBe 2
    }

    "return the correct contact with issued credentials" in {
      val institutionId = createParticipant("Institution X")
      val contact = createContact(institutionId, "Alice", None)

      val issuedCredential = createGenericCredential(
        issuedBy = institutionId,
        contactId = contact.contactId,
        tag = "tag1",
        credentialIssuanceContactId = None
      )
      publishCredential(institutionId, issuedCredential)

      val contactWithDetails =
        repository
          .find(institutionId, contact.contactId)
          .unsafeToFuture()
          .futureValue
          .value

      contactWithDetails.issuedCredentials.size mustBe 1
      contactWithDetails.issuedCredentials.head
        .copy(publicationData = None) mustBe issuedCredential
    }

    "return the correct contact with received credentials" in {
      val institutionId = createParticipant("Institution X")
      val contactA = createContact(institutionId, "Alice", None)
      createReceivedCredential(contactA.contactId)

      val contactWithDetails =
        repository
          .find(institutionId, contactA.contactId)
          .unsafeToFuture()
          .futureValue
          .value

      contactWithDetails.receivedCredentials.size mustBe 1
    }

    "return no contact when the contact is missing (institutionId and contactId not correlated)" in {
      val institutionXId = createParticipant("Institution X")
      val institutionYId = createParticipant("Institution Y")
      val groupNameA = createInstitutionGroup(
        institutionXId,
        InstitutionGroup.Name("Group A")
      ).name
      val groupNameB = createInstitutionGroup(
        institutionYId,
        InstitutionGroup.Name("Group B")
      ).name
      val contactA = createContact(institutionXId, "Alice", Some(groupNameA))
      createContact(institutionYId, "Bob", Some(groupNameB))

      val result =
        repository
          .find(institutionYId, contactA.contactId)
          .unsafeToFuture()
          .futureValue
      result must be(empty)
    }
  }

  "find by externalId" should {
    "return the correct contact when present" in {
      val institutionId = createParticipant("Institution X")
      val contactA = createContact(institutionId, "Alice", None)
      createContact(institutionId, "Bob", None)

      val result =
        repository
          .find(institutionId, contactA.externalId)
          .unsafeToFuture()
          .futureValue
          .value
      result must be(contactA)
    }

    "return no contact when the contact is missing (institutionId and contactId not correlated)" in {
      val institutionXId = createParticipant("Institution X")
      val institutionYId = createParticipant("Institution Y")
      val groupNameA = createInstitutionGroup(
        institutionXId,
        InstitutionGroup.Name("Group A")
      ).name
      val groupNameB = createInstitutionGroup(
        institutionYId,
        InstitutionGroup.Name("Group B")
      ).name
      val contactA = createContact(institutionXId, "Alice", Some(groupNameA))
      createContact(institutionYId, "Bob", Some(groupNameB))

      val result =
        repository
          .find(institutionYId, contactA.externalId)
          .unsafeToFuture()
          .futureValue
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
        val condition =
          if (desc) ResultOrdering.Direction.Descending
          else ResultOrdering.Direction.Ascending

        PaginatedQueryConstraints(
          limit = limit,
          ordering = ResultOrdering(sortBy, condition),
          scrollId = scrollId,
          filters = Some(Contact.FilterBy(groupName))
        )
      }

      // queries the in-memory data to verify results
      // NOTE: this doesn't filter by groups
      def query(
          data: List[Contact],
          constraints: Contact.PaginatedQuery
      ): List[Contact] = {
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
          case Some(scrollId) =>
            sortedProperly.dropWhile(_.contactId != scrollId).drop(1)
          case None => sortedProperly
        }

        paginated.take(constraints.limit)
      }

      s"[$tag] return the first contacts" in {
        val institutionId = createParticipant("Institution X")
        val groupNameA = createInstitutionGroup(
          institutionId,
          InstitutionGroup.Name("Group A")
        ).name
        val groupNameB = createInstitutionGroup(
          institutionId,
          InstitutionGroup.Name("Group B")
        ).name
        val groupNameC = createInstitutionGroup(
          institutionId,
          InstitutionGroup.Name("Group C")
        ).name
        val contactA = createContact(institutionId, "Alice", Some(groupNameA))
        val contactB = createContact(institutionId, "Bob", Some(groupNameB))
        val contactC = createContact(institutionId, "Charles", Some(groupNameC))
        val contactD = createContact(institutionId, "Alice 2", Some(groupNameA))

        val expected =
          query(List(contactA, contactB, contactC, contactD), buildQuery(2))
        val result = repository
          .getBy(institutionId, buildQuery(2))
          .unsafeToFuture()
          .futureValue
          .map(_.details)

        result must be(expected)
      }

      s"[$tag] return the first contacts matching a group" in {
        val institutionId = createParticipant("Institution X")
        val groupNameA = createInstitutionGroup(
          institutionId,
          InstitutionGroup.Name("Group A")
        ).name
        val groupNameB = createInstitutionGroup(
          institutionId,
          InstitutionGroup.Name("Group B")
        ).name
        val groupNameC = createInstitutionGroup(
          institutionId,
          InstitutionGroup.Name("Group C")
        ).name
        val contactA = createContact(institutionId, "Alice", Some(groupNameA))
        createContact(institutionId, "Bob", Some(groupNameB))
        createContact(institutionId, "Charles", Some(groupNameC))
        val contactD = createContact(institutionId, "Alice 2", Some(groupNameA))

        val expected =
          query(List(contactA, contactD), buildQuery(2, Some(groupNameA)))
        val result = repository
          .getBy(institutionId, buildQuery(2, Some(groupNameA)))
          .unsafeToFuture()
          .futureValue
          .map(_.details)

        result must be(expected)
      }

      s"[$tag] paginate by the last seen contact" in {
        val institutionId = createParticipant("Institution X")
        val groupNameA = createInstitutionGroup(
          institutionId,
          InstitutionGroup.Name("Group A")
        ).name
        val groupNameB = createInstitutionGroup(
          institutionId,
          InstitutionGroup.Name("Group B")
        ).name
        val groupNameC = createInstitutionGroup(
          institutionId,
          InstitutionGroup.Name("Group C")
        ).name
        val contactA = createContact(institutionId, "Alice", Some(groupNameA))
        val contactB = createContact(institutionId, "Bob", Some(groupNameB))
        val contactC = createContact(institutionId, "Charles", Some(groupNameC))
        val contactD = createContact(institutionId, "Alice 2", Some(groupNameA))

        val scrollId = contactB.contactId
        val result = repository
          .getBy(institutionId, buildQuery(1, None, Some(scrollId)))
          .unsafeToFuture()
          .futureValue
          .map(_.details)

        val expected = query(
          List(contactA, contactB, contactC, contactD),
          buildQuery(1, scrollId = Some(scrollId))
        )
        result must be(expected)
      }

      s"[$tag] paginate by the last seen contact matching by group" in {
        val institutionId = createParticipant("Institution X")
        val groupNameA = createInstitutionGroup(
          institutionId,
          InstitutionGroup.Name("Group A")
        ).name
        val groupNameB = createInstitutionGroup(
          institutionId,
          InstitutionGroup.Name("Group B")
        ).name
        val groupNameC = createInstitutionGroup(
          institutionId,
          InstitutionGroup.Name("Group C")
        ).name
        val contactA = createContact(institutionId, "Alice", Some(groupNameA))
        createContact(institutionId, "Bob", Some(groupNameB))
        createContact(institutionId, "Charles", Some(groupNameC))
        val contactD = createContact(institutionId, "Alice 2", Some(groupNameA))
        val scrollId = contactA.contactId

        val expected = query(
          List(contactA, contactD),
          buildQuery(1, scrollId = Some(scrollId))
        )
        val result = repository
          .getBy(institutionId, buildQuery(1, Some(groupNameA), Some(scrollId)))
          .unsafeToFuture()
          .futureValue
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
        nameOrExternalId: Option[String] = None,
        createdAt: Option[LocalDate] = None
    ): Contact.PaginatedQuery = {
      PaginatedQueryConstraints(
        limit = 10,
        ordering = ResultOrdering(Contact.SortBy.createdAt),
        filters = Some(
          Contact.FilterBy(
            nameOrExternalId = nameOrExternalId,
            createdAt = createdAt
          )
        )
      )
    }

    "return items matching the externalId" in {
      val institutionId = createParticipant("Institution X")
      val contactA = createContact(
        institutionId,
        "Alice",
        externalId = Contact.ExternalId("atala1")
      )
      val contactB = createContact(
        institutionId,
        "Bob",
        externalId = Contact.ExternalId("atala2")
      )
      createContact(
        institutionId,
        "Charles",
        externalId = Contact.ExternalId("atxala")
      )
      createContact(
        institutionId,
        "Alice 2",
        externalId = Contact.ExternalId("iohk2")
      )

      val expected = List(contactA, contactB).map(_.contactId).toSet
      val result = repository
        .getBy(institutionId, filterQuery(nameOrExternalId = Some("tala")))
        .unsafeToFuture()
        .futureValue
        .map(_.details)

      result.map(_.contactId).toSet must be(expected)
    }

    "return items matching the contact name" in {
      val institutionId = createParticipant("Institution X")
      val contactA = createContact(
        institutionId,
        "Charles Hoskinson",
        externalId = Contact.ExternalId("atala1")
      )
      val contactB = createContact(
        institutionId,
        "Charles H",
        externalId = Contact.ExternalId("atala2")
      )
      createContact(
        institutionId,
        "Carlos",
        externalId = Contact.ExternalId("iohk1")
      )
      createContact(
        institutionId,
        "Alice 2",
        externalId = Contact.ExternalId("iohk2")
      )

      val expected = List(contactA, contactB).map(_.contactId).toSet
      val result = repository
        .getBy(institutionId, filterQuery(nameOrExternalId = Some("harl")))
        .unsafeToFuture()
        .futureValue
        .map(_.details)

      result.map(_.contactId).toSet must be(expected)
    }

    "return items matching the createdAt" in {
      val institutionId = createParticipant("Institution X")
      val now = Instant.parse("2007-12-03T10:15:30.00Z")
      val contactA =
        createContact(
          institutionId,
          "Alice",
          externalId = Contact.ExternalId("atala1"),
          createdAt = Some(now)
        )
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
        .getBy(
          institutionId,
          filterQuery(createdAt = Some(LocalDate.parse("2007-12-03")))
        )
        .unsafeToFuture()
        .futureValue
        .map(_.details)

      result.map(_.contactId).toSet must be(expected)
    }

    "return items matching the createdAt and contact name" in {
      val institutionId = createParticipant("Institution X")
      val now = Instant.parse("2007-12-03T10:15:30.00Z")
      val contactA =
        createContact(
          institutionId,
          "iohk1",
          externalId = Contact.ExternalId("atala1"),
          createdAt = Some(now)
        )
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
        .getBy(
          institutionId,
          filterQuery(
            nameOrExternalId = Some("ioh"),
            createdAt = Some(LocalDate.parse("2007-12-03"))
          )
        )
        .unsafeToFuture()
        .futureValue
        .map(_.details)

      result.map(_.contactId).toSet must be(expected)
    }

    "return items matching the createdAt and externalId" in {
      val institutionId = createParticipant("Institution X")
      val now = Instant.parse("2007-12-03T10:15:30.00Z")
      val contactA =
        createContact(
          institutionId,
          "iohk1",
          externalId = Contact.ExternalId("atala1"),
          createdAt = Some(now)
        )
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
          filterQuery(
            nameOrExternalId = Some("atala"),
            createdAt = Some(LocalDate.parse("2007-12-03"))
          )
        )
        .unsafeToFuture()
        .futureValue
        .map(_.details)

      result.map(_.contactId).toSet must be(expected)
    }

    "return items matching the createdAt, externalId, and contact name" in {
      val institutionId = createParticipant("Institution X")
      val now = Instant.parse("2007-12-03T10:15:30.00Z")
      val contactA =
        createContact(
          institutionId,
          "iohk1",
          externalId = Contact.ExternalId("atala1"),
          createdAt = Some(now)
        )
      val contactB =
        createContact(
          institutionId,
          "atala2",
          externalId = Contact.ExternalId("iohk2"),
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
        "atalaX",
        externalId = Contact.ExternalId("no"),
        createdAt = Some(now.minus(Period.ofDays(1)))
      )

      val expected = List(contactA, contactB).map(_.contactId).toSet
      val result = repository
        .getBy(
          institutionId,
          filterQuery(
            nameOrExternalId = Some("atala"),
            createdAt = Some(LocalDate.parse("2007-12-03"))
          )
        )
        .unsafeToFuture()
        .futureValue
        .map(_.details)

      result.map(_.contactId).toSet must be(expected)
    }

    "return the credential counts" in {
      val institutionId = createParticipant("Institution X")
      val contactA = createContact(institutionId, "iohk1")
      val contactB = createContact(institutionId, "iohk2")

      DataPreparation.createGenericCredential(
        institutionId,
        contactA.contactId,
        "A"
      )
      DataPreparation.createGenericCredential(
        institutionId,
        contactA.contactId,
        "B"
      )
      DataPreparation.createGenericCredential(
        institutionId,
        contactA.contactId,
        "C"
      )
      DataPreparation.createReceivedCredential(contactA.contactId)
      DataPreparation.createReceivedCredential(contactA.contactId)
      DataPreparation.createGenericCredential(
        institutionId,
        contactB.contactId,
        "F"
      )

      val expected =
        Map((contactA.contactId, (3, 2)), (contactB.contactId, (1, 0)))
      val result = repository
        .getBy(institutionId, filterQuery())
        .unsafeToFuture()
        .futureValue

      result
        .map(r =>
          (
            r.contactId,
            (
              r.counts.numberOfCredentialsCreated,
              r.counts.numberOfCredentialsReceived
            )
          )
        )
        .toMap must be(expected)
    }
  }

  "delete" should {
    "delete the correct contact along with its credentials" in {
      val institutionId = createParticipant("Institution X")
      val contactA = createContact(institutionId, "iohk1")
      val contactB = createContact(institutionId, "iohk2")

      DataPreparation.createGenericCredential(
        institutionId,
        contactA.contactId,
        "A"
      )
      DataPreparation.createGenericCredential(
        institutionId,
        contactA.contactId,
        "B"
      )
      val contactBCredential1 = DataPreparation.createGenericCredential(
        institutionId,
        contactB.contactId,
        "C"
      )
      val contactBCredential2 = DataPreparation.createGenericCredential(
        institutionId,
        contactB.contactId,
        "D"
      )

      repository
        .delete(institutionId, contactA.contactId, deleteCredentials = true)
        .unsafeToFuture()
        .futureValue
        .toOption
        .value

      // Check that contact A was deleted
      repository
        .find(institutionId, contactA.contactId)
        .unsafeToFuture()
        .futureValue must be(None)

      // Check that contact B was not deleted
      repository
        .find(institutionId, contactB.contactId)
        .unsafeToFuture()
        .futureValue
        .map(_.contact) must be(Some(contactB))

      // Check that contact A credentials were deleted
      credentialsRepository
        .getBy(institutionId, contactA.contactId)
        .unsafeRunSync() must be(List.empty)

      // Check that contact B credentials were not deleted
      credentialsRepository
        .getBy(institutionId, contactB.contactId)
        .unsafeToFuture()
        .futureValue must be(List(contactBCredential1, contactBCredential2))
    }

    "delete the correct contact along without deleting its credentials" in {
      val institutionId = createParticipant("Institution X")
      val contactA = createContact(institutionId, "iohk1")
      val contactB = createContact(institutionId, "iohk2")

      val contactBCredential1 = DataPreparation.createGenericCredential(
        institutionId,
        contactB.contactId,
        "A"
      )
      val contactBCredential2 = DataPreparation.createGenericCredential(
        institutionId,
        contactB.contactId,
        "B"
      )

      repository
        .delete(institutionId, contactA.contactId, deleteCredentials = false)
        .unsafeToFuture()
        .futureValue
        .toOption
        .value

      // Check that contact A was deleted
      repository
        .find(institutionId, contactA.contactId)
        .unsafeToFuture()
        .futureValue must be(None)

      // Check that contact B was not deleted
      repository
        .find(institutionId, contactB.contactId)
        .unsafeToFuture()
        .futureValue
        .map(_.contact) must be(Some(contactB))

      // Check that contact B credentials were not deleted
      credentialsRepository
        .getBy(institutionId, contactB.contactId)
        .unsafeToFuture()
        .futureValue must be(List(contactBCredential1, contactBCredential2))
    }

    "fail to delete contact without deleting its existing credentials" in {
      val institutionId = createParticipant("Institution X")
      val contact = createContact(institutionId, "iohk")

      DataPreparation.createGenericCredential(
        institutionId,
        contact.contactId,
        "A"
      )
      DataPreparation.createGenericCredential(
        institutionId,
        contact.contactId,
        "B"
      )

      val result = repository
        .delete(institutionId, contact.contactId, deleteCredentials = false)
        .unsafeToFuture()
        .futureValue

      result must be(Left(ContactHasExistingCredentials(contact.contactId)))
    }

    "fail to delete contact belonging to a different institution" in {
      val institutionId1 = createParticipant("Institution X")
      val institutionId2 = createParticipant("Institution Y")
      val contact = createContact(institutionId1, "iohk")

      DataPreparation.createGenericCredential(
        institutionId1,
        contact.contactId,
        "A"
      )
      DataPreparation.createGenericCredential(
        institutionId1,
        contact.contactId,
        "B"
      )

      val result = repository
        .delete(institutionId2, contact.contactId, deleteCredentials = false)
        .unsafeToFuture()
        .futureValue

      result must be(
        Left(
          ContactsInstitutionsDoNotMatch(
            List(contact.contactId),
            institutionId2
          )
        )
      )
    }

    "fail to delete a non-existing contact" in {
      val institutionId = createParticipant("Institution X")
      val contactId = Contact.Id(UUID.randomUUID())

      val result = repository
        .delete(institutionId, contactId, deleteCredentials = false)
        .unsafeToFuture()
        .futureValue

      result must be(
        Left(ContactsInstitutionsDoNotMatch(List(contactId), institutionId))
      )
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
