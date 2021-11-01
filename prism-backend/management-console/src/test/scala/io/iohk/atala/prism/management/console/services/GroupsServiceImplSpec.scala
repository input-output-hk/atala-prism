package io.iohk.atala.prism.management.console.services

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import doobie.implicits._
import doobie.util.transactor.Transactor
import io.iohk.atala.prism.auth.SignedRpcRequest
import io.iohk.atala.prism.auth.grpc.GrpcAuthenticationHeaderParser
import io.iohk.atala.prism.crypto.EC.{INSTANCE => EC}
import io.iohk.atala.prism.logging.TraceId
import io.iohk.atala.prism.logging.TraceId.IOWithTraceIdContext
import io.iohk.atala.prism.identity.{PrismDid => DID}
import io.iohk.atala.prism.management.console.grpc.GroupsGrpcService
import io.iohk.atala.prism.management.console.models.PaginatedQueryConstraints.ResultOrdering
import io.iohk.atala.prism.management.console.models._
import io.iohk.atala.prism.protos.common_models
import io.iohk.atala.prism.management.console.repositories.daos.ParticipantsDAO
import io.iohk.atala.prism.management.console.repositories.{
  InstitutionGroupsRepository,
  ParticipantsRepository,
  RequestNoncesRepository
}
import io.iohk.atala.prism.management.console.{DataPreparation, ManagementConsoleAuthenticator}
import io.iohk.atala.prism.protos.console_api
import io.iohk.atala.prism.{DIDUtil, RpcSpecBase}
import io.iohk.atala.prism.utils.IOUtils._
import org.mockito.MockitoSugar._
import org.scalatest.OptionValues._
import tofu.logging.Logs

import java.util.UUID

class GroupsServiceImplSpec extends RpcSpecBase with DIDUtil {
  private val managementConsoleTestLogs: Logs[IO, IOWithTraceIdContext] =
    Logs.withContext[IO, IOWithTraceIdContext]

  private val usingApiAs = usingApiAsConstructor(
    new console_api.GroupsServiceGrpc.GroupsServiceBlockingStub(_, _)
  )
  private lazy val institutionGroupsRepository =
    InstitutionGroupsRepository.unsafe(
      dbLiftedToTraceIdIO,
      managementConsoleTestLogs
    )
  private lazy val participantsRepository =
    ParticipantsRepository.unsafe(
      dbLiftedToTraceIdIO,
      managementConsoleTestLogs
    )
  private lazy val requestNoncesRepository =
    RequestNoncesRepository.unsafe(
      dbLiftedToTraceIdIO,
      managementConsoleTestLogs
    )
  protected lazy val nodeMock =
    mock[io.iohk.atala.prism.protos.node_api.NodeServiceGrpc.NodeService]
  private lazy val authenticator =
    new ManagementConsoleAuthenticator(
      participantsRepository,
      requestNoncesRepository,
      nodeMock,
      GrpcAuthenticationHeaderParser
    )

  override def services =
    Seq(
      console_api.GroupsServiceGrpc
        .bindService(
          new GroupsGrpcService(
            GroupsService
              .unsafe(institutionGroupsRepository, managementConsoleTestLogs),
            authenticator
          ),
          executionContext
        )
    )

  private val getGroupsQuery: InstitutionGroup.PaginatedQuery =
    PaginatedQueryConstraints(ordering = ResultOrdering(InstitutionGroup.SortBy.Name))

  "createGroup" should {
    "create a group" in {
      val keyPair = EC.generateKeyPair()
      val publicKey = keyPair.getPublicKey
      val did = generateDid(publicKey)
      val institutionId = createParticipant(did)
      val newGroup = InstitutionGroup.Name("IOHK University")
      val request = console_api.CreateGroupRequest(newGroup.value)
      val rpcRequest = SignedRpcRequest.generate(keyPair, did, request)

      usingApiAs(rpcRequest) { serviceStub =>
        val response = serviceStub.createGroup(request)

        // the data is included
        response.group.value.name must be(newGroup.value)
        response.group.value.id mustNot be(empty)
        response.group.value.createdAt.isDefined mustBe true
        response.group.value.numberOfContacts must be(0)

        // the new group needs to exist
        val groups =
          institutionGroupsRepository
            .getBy(institutionId, getGroupsQuery)
            .run(TraceId.generateYOLO)
            .unsafeRunSync()
            .groups
        groups.map(_.value.name) must contain(newGroup)
      }
    }

    "create a group with initial contact list" in {
      val keyPair = EC.generateKeyPair()
      val publicKey = keyPair.getPublicKey
      val did = generateDid(publicKey)
      val institutionId = createParticipant(did)
      val newGroup = InstitutionGroup.Name("IOHK University")
      val contact1 = DataPreparation.createContact(institutionId)
      val contact2 = DataPreparation.createContact(institutionId)
      val request = console_api.CreateGroupRequest(
        newGroup.value,
        List(contact1.contactId.uuid.toString, contact2.contactId.uuid.toString)
      )
      val rpcRequest = SignedRpcRequest.generate(keyPair, did, request)

      usingApiAs(rpcRequest) { serviceStub =>
        val response = serviceStub.createGroup(request)

        // the data is included
        response.group.value.name must be(newGroup.value)
        response.group.value.id mustNot be(empty)
        response.group.value.createdAt.isDefined mustBe true
        response.group.value.numberOfContacts must be(2)

        // the new group needs to exist
        val groups =
          institutionGroupsRepository
            .getBy(institutionId, getGroupsQuery)
            .run(TraceId.generateYOLO)
            .unsafeRunSync()
            .groups
        val result = groups.find(_.value.name == newGroup).value
        result.value.name must be(newGroup)
        result.numberOfContacts must be(2)
      }
    }

    "fail to create a group with non-unique contact id list" in {
      val keyPair = EC.generateKeyPair()
      val publicKey = keyPair.getPublicKey
      val did = generateDid(publicKey)
      val institutionId = createParticipant(did)
      val newGroup = InstitutionGroup.Name("IOHK University")
      val contact1 = DataPreparation.createContact(institutionId)
      val contact2 = DataPreparation.createContact(institutionId)
      val request = console_api.CreateGroupRequest(
        newGroup.value,
        List(
          contact1.contactId.uuid.toString,
          contact1.contactId.uuid.toString,
          contact2.contactId.uuid.toString
        )
      )
      val rpcRequest = SignedRpcRequest.generate(keyPair, did, request)

      usingApiAs(rpcRequest) { serviceStub =>
        intercept[RuntimeException](
          serviceStub.createGroup(request)
        )
      }
    }

    "fail to create a group when one of the contacts does not belong to the group institution" in {
      val keyPair1 = EC.generateKeyPair()
      val publicKey1 = keyPair1.getPublicKey
      val did1 = generateDid(publicKey1)
      val institutionId1 = createParticipant(did1)
      val keyPair2 = EC.generateKeyPair()
      val publicKey2 = keyPair2.getPublicKey
      val did2 = generateDid(publicKey2)
      val institutionId2 = createParticipant(did2)
      val newGroup = InstitutionGroup.Name("IOHK University")
      val contact1 = DataPreparation.createContact(institutionId1)
      val contact2 = DataPreparation.createContact(institutionId2)
      val request = console_api.CreateGroupRequest(
        newGroup.value,
        List(contact1.contactId.uuid.toString, contact2.contactId.uuid.toString)
      )
      val rpcRequest = SignedRpcRequest.generate(keyPair1, did1, request)

      usingApiAs(rpcRequest) { serviceStub =>
        intercept[RuntimeException](
          serviceStub.createGroup(request)
        )
      }
    }
  }

  "getGroups" should {
    "return available groups" in {
      assertGetGroupsResult(
        console_api.GetGroupsRequest(),
        List("Group 1", "Group 2", "Group 3")
      )
    }

    "return the contact count on each group" in {
      val keyPair = EC.generateKeyPair()
      val publicKey = keyPair.getPublicKey
      val did = generateDid(publicKey)
      val institutionId = createParticipant(did)

      val groups =
        List("Blockchain 2020", "Finance 2020").map(InstitutionGroup.Name.apply)
      groups.foreach { group =>
        institutionGroupsRepository
          .create(institutionId, group, Set())
          .run(TraceId.generateYOLO)
          .unsafeRunSync()
          .toOption
          .value
      }
      DataPreparation.createContact(institutionId, groupName = Some(groups(0)))
      DataPreparation.createContact(institutionId, groupName = Some(groups(0)))
      DataPreparation.createContact(institutionId, groupName = Some(groups(1)))

      val request = console_api.GetGroupsRequest()
      val rpcRequest = SignedRpcRequest.generate(keyPair, did, request)

      usingApiAs(rpcRequest) { serviceStub =>
        val result = serviceStub.getGroups(request).groups

        result.map(_.numberOfContacts) must be(List(2, 1))
      }
    }

    "allows filtering by contact" in {
      val keyPair = EC.generateKeyPair()
      val publicKey = keyPair.getPublicKey
      val did = generateDid(publicKey)
      val issuerId = createParticipant(did)

      val groups =
        List("Blockchain 2020", "Finance 2020").map(InstitutionGroup.Name.apply)
      groups.foreach { group =>
        institutionGroupsRepository
          .create(issuerId, group, Set())
          .run(TraceId.generateYOLO)
          .unsafeRunSync()
          .toOption
          .value
      }
      DataPreparation.createContact(issuerId, groupName = Some(groups(0)))
      val contact =
        DataPreparation.createContact(issuerId, groupName = Some(groups(0)))
      DataPreparation.createContact(issuerId, groupName = Some(groups(1)))

      val request = console_api
        .GetGroupsRequest()
        .withFilterBy(
          console_api.GetGroupsRequest
            .FilterBy(contactId = contact.contactId.toString)
        )
      val rpcRequest = SignedRpcRequest.generate(keyPair, did, request)

      usingApiAs(rpcRequest) { serviceStub =>
        val result = serviceStub.getGroups(request).groups
        result.size must be(1)

        val resultGroup = result.head
        resultGroup.id mustNot be(empty)
        resultGroup.createdAt.isDefined mustBe true
        resultGroup.name must be(groups(0).value)
        resultGroup.numberOfContacts must be(2)
      }
    }

    "allows filtering by created after" in {
      val request = console_api
        .GetGroupsRequest()
        .withFilterBy(
          console_api.GetGroupsRequest
            .FilterBy(createdAfter = Some(common_models.Date(2021, 3, 15)))
        )
      assertGetGroupsResult(request, List("Group 1", "Group 2", "Group 3"))
    }

    "allows filtering by created before" in {
      val request = console_api
        .GetGroupsRequest()
        .withFilterBy(
          console_api.GetGroupsRequest
            .FilterBy(createdBefore = Some(common_models.Date(2021, 3, 15)))
        )
      assertGetGroupsResult(request, List.empty)
    }

    "allows filtering by name" in {
      val request = console_api
        .GetGroupsRequest()
        .withFilterBy(console_api.GetGroupsRequest.FilterBy(name = "Up 3"))
      assertGetGroupsResult(request, List("Group 3"))
    }

    "allows sorting by created name desc" in {
      val request = console_api
        .GetGroupsRequest()
        .withSortBy(
          console_api.GetGroupsRequest.SortBy(
            field = console_api.GetGroupsRequest.SortBy.Field.NAME,
            direction = common_models.SortByDirection.SORT_BY_DIRECTION_DESCENDING
          )
        )
      assertGetGroupsResult(request, List("Group 3", "Group 2", "Group 1"))
    }

    "respect limit and offset" in {
      val request = console_api
        .GetGroupsRequest()
        .withLimit(1)
        .withOffset(1)

      assertGetGroupsResult(request, List("Group 2"))
    }

    def assertGetGroupsResult(
        request: console_api.GetGroupsRequest,
        expectedResult: List[String]
    ) = {
      val keyPair = EC.generateKeyPair()
      val publicKey = keyPair.getPublicKey
      val did = generateDid(publicKey)
      val institutionId = createParticipant(did)

      val groups =
        List("Group 1", "Group 2", "Group 3").map(InstitutionGroup.Name.apply)
      groups.foreach { group =>
        institutionGroupsRepository
          .create(institutionId, group, Set())
          .run(TraceId.generateYOLO)
          .unsafeRunSync()
          .toOption
          .value
      }

      val rpcRequest = SignedRpcRequest.generate(keyPair, did, request)

      usingApiAs(rpcRequest) { serviceStub =>
        val result = serviceStub.getGroups(request)
        result.groups.map(_.name) mustBe expectedResult
      }
    }

    "fails to filter by contact when the value is invalid" in {
      val request =
        console_api
          .GetGroupsRequest()
          .withFilterBy(
            console_api.GetGroupsRequest.FilterBy(contactId = "xyz")
          )
      assertRequestFails(request)
    }

    "fails to filter by created after date when the value is invalid" in {
      val request = console_api
        .GetGroupsRequest()
        .withFilterBy(
          console_api.GetGroupsRequest.FilterBy(createdAfter = Some(common_models.Date(2021, 3, day = 40)))
        )
      assertRequestFails(request)
    }

    "fails to filter by created before date when the value is invalid" in {
      val request = console_api
        .GetGroupsRequest()
        .withFilterBy(
          console_api.GetGroupsRequest.FilterBy(createdBefore = Some(common_models.Date(2021, 3, day = 40)))
        )
      assertRequestFails(request)
    }

    "fails to respect limit when limit value is invalid" in {
      val request = console_api.GetGroupsRequest().withLimit(101)
      assertRequestFails(request)
    }

    "fails to respect offset when offset value is negative" in {
      val request = console_api.GetGroupsRequest().withOffset(-5)
      assertRequestFails(request)
    }

    def assertRequestFails(request: console_api.GetGroupsRequest) = {
      val keyPair = EC.generateKeyPair()
      val publicKey = keyPair.getPublicKey
      val did = generateDid(publicKey)
      createParticipant(did)

      val rpcRequest = SignedRpcRequest.generate(keyPair, did, request)

      usingApiAs(rpcRequest) { serviceStub =>
        assertThrows[RuntimeException] {
          serviceStub.getGroups(request)
        }
      }
    }
  }

  "updateGroup" should {
    val group1 = "Group 1"
    val group1Name = InstitutionGroup.Name(group1)
    val group2 = "Group 2"
    val group2Name = InstitutionGroup.Name(group2)

    "be able to add new contacts" in {
      val keyPair = EC.generateKeyPair()
      val publicKey = keyPair.getPublicKey
      val did = generateDid(publicKey)
      val institutionId = createParticipant(did)

      val groupNames = List(group1Name, group2Name)
      val List(group1Id, _) = groupNames.map { groupName =>
        institutionGroupsRepository
          .create(institutionId, groupName, Set())
          .run(TraceId.generateYOLO)
          .unsafeRunSync()
          .toOption
          .value
          .id
          .toString
      }
      val contact = DataPreparation.createContact(institutionId)

      val request1 =
        console_api.UpdateGroupRequest(
          group1Id,
          Seq(contact.contactId.toString),
          Seq()
        )
      val rpcRequest1 = SignedRpcRequest.generate(keyPair, did, request1)

      usingApiAs(rpcRequest1) { serviceStub =>
        serviceStub.updateGroup(request1)
        listContacts(institutionId, group1Name) must be(List(contact))
        listContacts(institutionId, group2Name) must be(List())
      }

      // Adding the same contact twice should have no effect
      val request2 =
        console_api.UpdateGroupRequest(
          group1Id,
          Seq(contact.contactId.toString),
          Seq()
        )
      val rpcRequest2 = SignedRpcRequest.generate(keyPair, did, request2)

      usingApiAs(rpcRequest2) { serviceStub =>
        serviceStub.updateGroup(request2)
        listContacts(institutionId, group1Name) must be(List(contact))
        listContacts(institutionId, group2Name) must be(List())
      }
    }

    "be able to remove contacts" in {
      val keyPair = EC.generateKeyPair()
      val publicKey = keyPair.getPublicKey
      val did = generateDid(publicKey)
      val institutionId = createParticipant(did)

      val groupNames = List(group1Name, group2Name)
      val List(group1Id, _) = groupNames.map { groupName =>
        institutionGroupsRepository
          .create(institutionId, groupName, Set())
          .run(TraceId.generateYOLO)
          .unsafeRunSync()
          .toOption
          .value
          .id
          .toString
      }
      val contact1 = DataPreparation.createContact(
        institutionId,
        groupName = Some(group1Name)
      )
      val contact2 = DataPreparation.createContact(
        institutionId,
        groupName = Some(group2Name)
      )

      listContacts(institutionId, group1Name) must be(List(contact1))
      listContacts(institutionId, group2Name) must be(List(contact2))

      val request1 =
        console_api.UpdateGroupRequest(
          group1Id,
          Seq(),
          Seq(contact1.contactId.toString)
        )
      val rpcRequest1 = SignedRpcRequest.generate(keyPair, did, request1)

      usingApiAs(rpcRequest1) { serviceStub =>
        serviceStub.updateGroup(request1)

        listContacts(institutionId, group1Name) must be(List())
        listContacts(institutionId, group2Name) must be(List(contact2))
      }

      // Removing the same contact twice should have no effect
      val request2 =
        console_api.UpdateGroupRequest(
          group1Id,
          Seq(),
          Seq(contact1.contactId.toString)
        )
      val rpcRequest2 = SignedRpcRequest.generate(keyPair, did, request2)

      usingApiAs(rpcRequest2) { serviceStub =>
        serviceStub.updateGroup(request2)

        listContacts(institutionId, group1Name) must be(List())
        listContacts(institutionId, group2Name) must be(List(contact2))
      }
    }

    "be able to add and remove at the same time" in {
      val keyPair = EC.generateKeyPair()
      val publicKey = keyPair.getPublicKey
      val did = generateDid(publicKey)
      val institutionId = createParticipant(did)

      val groupNames = List(group1Name, group2Name)
      val List(group1Id, _) = groupNames.map { groupName =>
        institutionGroupsRepository
          .create(institutionId, groupName, Set())
          .run(TraceId.generateYOLO)
          .unsafeRunSync()
          .toOption
          .value
          .id
          .toString
      }
      val contact1 = DataPreparation.createContact(
        institutionId,
        groupName = Some(group1Name)
      )
      val contact2 = DataPreparation.createContact(institutionId)

      listContacts(institutionId, group1Name) must be(List(contact1))
      listContacts(institutionId, group2Name) must be(List())

      val request1 =
        console_api.UpdateGroupRequest(
          group1Id,
          Seq(contact2.contactId.toString),
          Seq(contact1.contactId.toString)
        )
      val rpcRequest1 = SignedRpcRequest.generate(keyPair, did, request1)

      usingApiAs(rpcRequest1) { serviceStub =>
        serviceStub.updateGroup(request1)

        listContacts(institutionId, group1Name) must be(List(contact2))
        listContacts(institutionId, group2Name) must be(List())
      }

      // Adding and removing the same contact at the same time should have no effect
      val request2 =
        console_api.UpdateGroupRequest(
          group1Id,
          Seq(contact1.contactId.toString),
          Seq(contact1.contactId.toString)
        )
      val rpcRequest2 = SignedRpcRequest.generate(keyPair, did, request2)

      usingApiAs(rpcRequest2) { serviceStub =>
        serviceStub.updateGroup(request2)

        listContacts(institutionId, group1Name) must be(List(contact2))
        listContacts(institutionId, group2Name) must be(List())
      }
    }

    "reject requests with non-matching group institution" in {
      val keyPair1 = EC.generateKeyPair()
      val publicKey1 = keyPair1.getPublicKey
      val did1 = generateDid(publicKey1)
      val institutionId1 = createParticipant(did1)
      val keyPair2 = EC.generateKeyPair()
      val publicKey2 = keyPair2.getPublicKey
      val did2 = generateDid(publicKey2)
      val institutionId2 = createParticipant(did2)

      val group1Id =
        institutionGroupsRepository
          .create(institutionId1, group1Name, Set())
          .run(TraceId.generateYOLO)
          .unsafeRunSync()
          .toOption
          .value
          .id
          .toString
      val contact = DataPreparation.createContact(institutionId2)

      val request =
        console_api.UpdateGroupRequest(
          group1Id,
          Seq(contact.contactId.toString),
          Seq()
        )
      val rpcRequest = SignedRpcRequest.generate(keyPair2, did2, request)

      usingApiAs(rpcRequest) { serviceStub =>
        intercept[RuntimeException](
          serviceStub.updateGroup(request)
        )
      }
    }

    "reject requests with non-matching contact institution" in {
      val keyPair1 = EC.generateKeyPair()
      val publicKey1 = keyPair1.getPublicKey
      val did1 = generateDid(publicKey1)
      val institutionId1 = createParticipant(did1)
      val keyPair2 = EC.generateKeyPair()
      val publicKey2 = keyPair2.getPublicKey
      val did2 = generateDid(publicKey2)
      val institutionId2 = createParticipant(did2)

      val group1Id =
        institutionGroupsRepository
          .create(institutionId1, group1Name, Set())
          .run(TraceId.generateYOLO)
          .unsafeRunSync()
          .toOption
          .value
          .id
          .toString
      val contact = DataPreparation.createContact(institutionId2)

      val request =
        console_api.UpdateGroupRequest(
          group1Id,
          Seq(contact.contactId.toString),
          Seq()
        )
      val rpcRequest = SignedRpcRequest.generate(keyPair1, did1, request)

      usingApiAs(rpcRequest) { serviceStub =>
        intercept[RuntimeException](
          serviceStub.updateGroup(request)
        )
      }
    }

    "be able to change the group's name" in {
      val keyPair = EC.generateKeyPair()
      val publicKey = keyPair.getPublicKey
      val did = generateDid(publicKey)
      val institutionId = createParticipant(did)

      val groupNames = List(group1Name, group2Name)
      val List(group1Id, _) = groupNames.map { groupName =>
        institutionGroupsRepository
          .create(institutionId, groupName, Set())
          .run(TraceId.generateYOLO)
          .unsafeRunSync()
          .toOption
          .value
          .id
          .toString
      }
      val newName = "New Group"

      val request =
        console_api.UpdateGroupRequest(group1Id, name = newName)
      val rpcRequest = SignedRpcRequest.generate(keyPair, did, request)

      usingApiAs(rpcRequest) { serviceStub =>
        serviceStub.updateGroup(request)

        // Check that the group was indeed renamed
        val groups =
          institutionGroupsRepository
            .getBy(institutionId, getGroupsQuery)
            .run(TraceId.generateYOLO)
            .unsafeRunSync()
            .groups
        groups.map(_.value.name.value) must contain(newName)
      }
    }

    "reject when renaming into an already existing group name" in {
      val keyPair = EC.generateKeyPair()
      val publicKey = keyPair.getPublicKey
      val did = generateDid(publicKey)
      val institutionId = createParticipant(did)

      val groupNames = List(group1Name, group2Name)
      val List(group1Id, _) = groupNames.map { groupName =>
        institutionGroupsRepository
          .create(institutionId, groupName, Set())
          .run(TraceId.generateYOLO)
          .unsafeRunSync()
          .toOption
          .value
          .id
          .toString
      }

      val request =
        console_api.UpdateGroupRequest(group1Id, name = group2Name.value)
      val rpcRequest = SignedRpcRequest.generate(keyPair, did, request)

      usingApiAs(rpcRequest) { serviceStub =>
        intercept[RuntimeException] {
          serviceStub.updateGroup(request)
        }
      }
    }
  }

  "copyGroup" should {
    val originalGroup = "Group1"
    val originalGroupName = InstitutionGroup.Name(originalGroup)
    val newGroup = "newGroup"
    val newGroupName = InstitutionGroup.Name(newGroup)
    "be able to copy group with contacts in it" in {
      val keyPair = EC.generateKeyPair()
      val publicKey = keyPair.getPublicKey
      val did = generateDid(publicKey)
      val institutionId = createParticipant(did)

      val originalGroupId = createGroup(institutionId, originalGroupName)
      val contact1 = DataPreparation.createContact(
        institutionId,
        groupName = Some(originalGroupName)
      )
      val contact2 = DataPreparation.createContact(
        institutionId,
        groupName = Some(originalGroupName)
      )
      val contact3 = DataPreparation.createContact(
        institutionId,
        groupName = Some(originalGroupName)
      )
      val contact4 = DataPreparation.createContact(
        institutionId,
        groupName = Some(originalGroupName)
      )

      // To ensure there is only one group at this point
      getInstitutionGroups(institutionId).size mustBe 1

      // To guarantee that these contacts are added
      listContacts(institutionId, originalGroupName).size mustBe 4

      val requestCopy =
        console_api.CopyGroupRequest(originalGroupId.toString, newGroup)
      val rpcRequestCopy = SignedRpcRequest.generate(keyPair, did, requestCopy)

      usingApiAs(rpcRequestCopy) { serviceStub =>
        val newGroupStringId = serviceStub.copyGroup(requestCopy).groupId
        val newGroupId = UUID.fromString(newGroupStringId)
        // To ensure that all contacts are copied
        listContacts(institutionId, newGroupName) mustBe List(
          contact1,
          contact2,
          contact3,
          contact4
        )
        // And the group itself
        getInstitutionGroups(institutionId)
          .map(_.value.id) mustBe List(
          originalGroupId,
          InstitutionGroup.Id(newGroupId)
        )
      }
    }

    "be able to copy group without any contacts" in {
      val keyPair = EC.generateKeyPair()
      val publicKey = keyPair.getPublicKey
      val did = generateDid(publicKey)
      val institutionId = createParticipant(did)

      val originalGroupId = createGroup(institutionId, originalGroupName)

      // To ensure the group is empty
      listContacts(institutionId, originalGroupName).isEmpty mustBe true

      val requestCopy =
        console_api.CopyGroupRequest(originalGroupId.toString, newGroup)
      val rpcRequestCopy = SignedRpcRequest.generate(keyPair, did, requestCopy)

      usingApiAs(rpcRequestCopy) { serviceStub =>
        val newGroupStringId = serviceStub.copyGroup(requestCopy).groupId
        val newGroupId = UUID.fromString(newGroupStringId)
        // To check that new group is empty too
        listContacts(institutionId, newGroupName).isEmpty mustBe true
        // And the group was copied with the same institutionId
        getInstitutionGroups(institutionId)
          .map(_.value.id) mustBe List(
          originalGroupId,
          InstitutionGroup.Id(newGroupId)
        )
      }
    }

    "reject group copying if there is wrong institution id" in {
      val keyPair = EC.generateKeyPair()
      val imposterKeyPair = EC.generateKeyPair()
      val publicKey = keyPair.getPublicKey
      val imposterPublicKey = imposterKeyPair.getPublicKey
      val did = generateDid(publicKey)
      val imposterDid = generateDid(imposterPublicKey)
      val institutionId = createParticipant(did)
      createParticipant(imposterDid)

      val originalGroupId = createGroup(institutionId, originalGroupName)

      val requestCopy =
        console_api.CopyGroupRequest(originalGroupId.toString, newGroup)
      val rpcRequestCopy =
        SignedRpcRequest.generate(imposterKeyPair, imposterDid, requestCopy)

      usingApiAs(rpcRequestCopy) { serviceStub =>
        intercept[RuntimeException](serviceStub.copyGroup(requestCopy))
      }
    }

    "reject group copying if there is empty name" in {
      val keyPair = EC.generateKeyPair()
      val publicKey = keyPair.getPublicKey
      val did = generateDid(publicKey)
      val institutionId = createParticipant(did)

      val originalGroupId = createGroup(institutionId, originalGroupName)

      val requestCopy =
        console_api.CopyGroupRequest(originalGroupId.toString, "")
      val rpcRequestCopy = SignedRpcRequest.generate(keyPair, did, requestCopy)

      usingApiAs(rpcRequestCopy) { serviceStub =>
        intercept[RuntimeException](serviceStub.copyGroup(requestCopy))
      }
    }

    "reject group copying if the chosen name is already occupied" in {
      val keyPair = EC.generateKeyPair()
      val publicKey = keyPair.getPublicKey
      val did = generateDid(publicKey)
      val institutionId = createParticipant(did)

      val originalGroupId = createGroup(institutionId, originalGroupName)
      createGroup(institutionId, newGroupName)

      val requestCopy = console_api.CopyGroupRequest(
        originalGroupId.toString,
        newGroupName.value
      )
      val rpcRequestCopy = SignedRpcRequest.generate(keyPair, did, requestCopy)

      usingApiAs(rpcRequestCopy) { serviceStub =>
        intercept[RuntimeException](serviceStub.copyGroup(requestCopy))
      }
    }
  }

  "deleteGroup" should {
    val group1 = "Group1"
    val group1Name = InstitutionGroup.Name(group1)
    val group2 = "Group2"
    val group2Name = InstitutionGroup.Name(group2)

    "be able to delete group with contacts in it" in {
      val keyPair = EC.generateKeyPair()
      val publicKey = keyPair.getPublicKey
      val did = generateDid(publicKey)
      val institutionId = createParticipant(did)

      val group1Id = createGroup(institutionId, group1Name)
      val group2Id = createGroup(institutionId, group2Name)

      DataPreparation.createContact(institutionId, groupName = Some(group1Name))
      DataPreparation.createContact(institutionId, groupName = Some(group1Name))
      DataPreparation.createContact(institutionId, groupName = Some(group2Name))
      DataPreparation.createContact(institutionId, groupName = Some(group2Name))

      // To ensure that these groups and contacts are added
      listContacts(institutionId, group1Name).size mustBe 2
      listContacts(institutionId, group2Name).size mustBe 2
      getInstitutionGroups(institutionId).map(
        _.value.id
      ) must contain theSameElementsAs List(group1Id, group2Id)

      val requestDelete = console_api.DeleteGroupRequest(group1Id.toString)
      val rpcRequestDelete =
        SignedRpcRequest.generate(keyPair, did, requestDelete)

      usingApiAs(rpcRequestDelete) { serviceStub =>
        serviceStub.deleteGroup(requestDelete)
        // Exception as the proof that we can't get contacts for the group which doesn't exist anymore
        intercept[RuntimeException](listContacts(institutionId, group1Name))
        // Also, to ensure that the second group and its contacts still exist
        listContacts(institutionId, group2Name).size mustBe 2
        getInstitutionGroups(institutionId).map(
          _.value.id
        ) must not contain group1Id
      }
    }

    "reject group deleting if here is wrong institution id" in {
      val keyPair = EC.generateKeyPair()
      val imposterKeyPair = EC.generateKeyPair()
      val publicKey = keyPair.getPublicKey
      val imposterPublicKey = imposterKeyPair.getPublicKey
      val did = generateDid(publicKey)
      val imposterDid = generateDid(imposterPublicKey)
      val institutionId = createParticipant(did)
      createParticipant(imposterDid)

      val group1Id = createGroup(institutionId, group1Name)

      DataPreparation.createContact(institutionId, groupName = Some(group1Name))
      DataPreparation.createContact(institutionId, groupName = Some(group1Name))

      val requestDelete = console_api.DeleteGroupRequest(group1Id.toString)
      val rpcRequestDelete =
        SignedRpcRequest.generate(imposterKeyPair, imposterDid, requestDelete)

      usingApiAs(rpcRequestDelete) { serviceStub =>
        intercept[RuntimeException](serviceStub.deleteGroup(requestDelete))
      }
    }

    "reject group deleting if a group doesn't exist" in {
      val keyPair = EC.generateKeyPair()
      val publicKey = keyPair.getPublicKey
      val did = generateDid(publicKey)
      createParticipant(did)

      val requestDelete =
        console_api.DeleteGroupRequest(UUID.randomUUID().toString)
      val rpcRequestDelete =
        SignedRpcRequest.generate(keyPair, did, requestDelete)

      usingApiAs(rpcRequestDelete) { serviceStub =>
        intercept[RuntimeException](serviceStub.deleteGroup(requestDelete))
      }
    }
  }

  private def listContacts(
      institutionId: ParticipantId,
      groupName: InstitutionGroup.Name
  ): List[Contact] =
    institutionGroupsRepository
      .listContacts(institutionId, groupName)
      .run(TraceId.generateYOLO)
      .unsafeRunSync()

  private def createParticipant(did: DID)(implicit
      database: Transactor[IO]
  ): ParticipantId = {
    val id = ParticipantId.random()

    val participant =
      ParticipantInfo(
        id,
        "",
        did,
        Some(ParticipantLogo(Vector()))
      )
    ParticipantsDAO.insert(participant).transact(database).unsafeRunSync()

    id
  }

  private def getInstitutionGroups(
      institutionId: ParticipantId
  ): List[InstitutionGroup.WithContactCount] = {
    val groups = institutionGroupsRepository
      .getBy(institutionId, getGroupsQuery)
      .run(TraceId.generateYOLO)
      .unsafeRunSync()
      .groups

    groups
  }

  private def createGroup(
      institutionId: ParticipantId,
      name: InstitutionGroup.Name,
      contactIds: Set[Contact.Id] = Set.empty
  ): InstitutionGroup.Id =
    institutionGroupsRepository
      .create(institutionId, name, contactIds)
      .run(TraceId.generateYOLO)
      .unsafeRunSync()
      .toOption
      .value
      .id
}
