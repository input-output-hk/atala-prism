package io.iohk.atala.prism.management.console.services

import cats.effect.IO
import doobie.implicits._
import doobie.util.transactor.Transactor
import io.iohk.atala.prism.auth.SignedRpcRequest
import io.iohk.atala.prism.auth.grpc.GrpcAuthenticationHeaderParser
import io.iohk.atala.prism.crypto.EC
import io.iohk.atala.prism.identity.DID
import io.iohk.atala.prism.management.console.models._
import io.iohk.atala.prism.management.console.repositories.daos.ParticipantsDAO
import io.iohk.atala.prism.management.console.repositories.{
  InstitutionGroupsRepository,
  ParticipantsRepository,
  RequestNoncesRepository
}
import io.iohk.atala.prism.management.console.{DataPreparation, ManagementConsoleAuthenticator}
import io.iohk.atala.prism.protos.console_api
import io.iohk.atala.prism.{DIDGenerator, RpcSpecBase}
import org.mockito.MockitoSugar._
import org.scalatest.OptionValues._

class GroupsServiceImplSpec extends RpcSpecBase with DIDGenerator {
  private val usingApiAs = usingApiAsConstructor(new console_api.GroupsServiceGrpc.GroupsServiceBlockingStub(_, _))

  private lazy val institutionGroupsRepository = new InstitutionGroupsRepository(database)
  private lazy val participantsRepository = new ParticipantsRepository(database)
  private lazy val requestNoncesRepository = new RequestNoncesRepository.PostgresImpl(database)(executionContext)
  protected lazy val nodeMock = mock[io.iohk.atala.prism.protos.node_api.NodeServiceGrpc.NodeService]
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
        .bindService(new GroupsServiceImpl(institutionGroupsRepository, authenticator), executionContext)
    )

  "createGroup" should {
    "create a group" in {
      val keyPair = EC.generateKeyPair()
      val publicKey = keyPair.publicKey
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
        response.group.value.createdAt > 0 must be(true)
        response.group.value.numberOfContacts must be(0)

        // the new group needs to exist
        val groups = institutionGroupsRepository.getBy(institutionId, None).value.futureValue.toOption.value
        groups.map(_.value.name) must contain(newGroup)
      }
    }

    "create a group with initial contact list" in {
      val keyPair = EC.generateKeyPair()
      val publicKey = keyPair.publicKey
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
        response.group.value.createdAt > 0 must be(true)
        response.group.value.numberOfContacts must be(2)

        // the new group needs to exist
        val groups = institutionGroupsRepository.getBy(institutionId, None).value.futureValue.toOption.value
        val result = groups.find(_.value.name == newGroup).value
        result.value.name must be(newGroup)
        result.numberOfContacts must be(2)
      }
    }

    "fail to create a group with non-unique contact id list" in {
      val keyPair = EC.generateKeyPair()
      val publicKey = keyPair.publicKey
      val did = generateDid(publicKey)
      val institutionId = createParticipant(did)
      val newGroup = InstitutionGroup.Name("IOHK University")
      val contact1 = DataPreparation.createContact(institutionId)
      val contact2 = DataPreparation.createContact(institutionId)
      val request = console_api.CreateGroupRequest(
        newGroup.value,
        List(contact1.contactId.uuid.toString, contact1.contactId.uuid.toString, contact2.contactId.uuid.toString)
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
      val publicKey1 = keyPair1.publicKey
      val did1 = generateDid(publicKey1)
      val institutionId1 = createParticipant(did1)
      val keyPair2 = EC.generateKeyPair()
      val publicKey2 = keyPair2.publicKey
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
      val keyPair = EC.generateKeyPair()
      val publicKey = keyPair.publicKey
      val did = generateDid(publicKey)
      val institutionId = createParticipant(did)

      val groups = List("Blockchain 2020", "Finance 2020").map(InstitutionGroup.Name.apply)
      groups.foreach { group =>
        institutionGroupsRepository.create(institutionId, group, Set()).value.futureValue.toOption.value
      }

      val request = console_api.GetGroupsRequest()
      val rpcRequest = SignedRpcRequest.generate(keyPair, did, request)

      usingApiAs(rpcRequest) { serviceStub =>
        val result = serviceStub.getGroups(request)
        result.groups.map(_.name).map(InstitutionGroup.Name.apply) must be(groups)
      }
    }

    "return the contact count on each group" in {
      val keyPair = EC.generateKeyPair()
      val publicKey = keyPair.publicKey
      val did = generateDid(publicKey)
      val institutionId = createParticipant(did)

      val groups = List("Blockchain 2020", "Finance 2020").map(InstitutionGroup.Name.apply)
      groups.foreach { group =>
        institutionGroupsRepository.create(institutionId, group, Set()).value.futureValue.toOption.value
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
      val publicKey = keyPair.publicKey
      val did = generateDid(publicKey)
      val issuerId = createParticipant(did)

      val groups = List("Blockchain 2020", "Finance 2020").map(InstitutionGroup.Name.apply)
      groups.foreach { group =>
        institutionGroupsRepository.create(issuerId, group, Set()).value.futureValue.toOption.value
      }
      DataPreparation.createContact(issuerId, groupName = Some(groups(0)))
      val contact = DataPreparation.createContact(issuerId, groupName = Some(groups(0)))
      DataPreparation.createContact(issuerId, groupName = Some(groups(1)))

      val request = console_api.GetGroupsRequest().withContactId(contact.contactId.toString)
      val rpcRequest = SignedRpcRequest.generate(keyPair, did, request)

      usingApiAs(rpcRequest) { serviceStub =>
        val result = serviceStub.getGroups(request).groups
        result.size must be(1)

        val resultGroup = result.head
        resultGroup.id mustNot be(empty)
        resultGroup.createdAt > 0 must be(true)
        resultGroup.name must be(groups(0).value)
        resultGroup.numberOfContacts must be(2)
      }
    }

    "fails to filter by contact when the value is invalid" in {
      val keyPair = EC.generateKeyPair()
      val publicKey = keyPair.publicKey
      val did = generateDid(publicKey)
      createParticipant(did)

      val request = console_api.GetGroupsRequest().withContactId("xyz")
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
      val publicKey = keyPair.publicKey
      val did = generateDid(publicKey)
      val institutionId = createParticipant(did)

      val groupNames = List(group1Name, group2Name)
      val List(group1Id, _) = groupNames.map { groupName =>
        institutionGroupsRepository
          .create(institutionId, groupName, Set())
          .value
          .futureValue
          .toOption
          .value
          .id
          .toString
      }
      val contact = DataPreparation.createContact(institutionId)

      val request1 =
        console_api.UpdateGroupRequest(group1Id, Seq(contact.contactId.toString), Seq())
      val rpcRequest1 = SignedRpcRequest.generate(keyPair, did, request1)

      usingApiAs(rpcRequest1) { serviceStub =>
        serviceStub.updateGroup(request1)
        listContacts(institutionId, group1Name) must be(List(contact))
        listContacts(institutionId, group2Name) must be(List())
      }

      // Adding the same contact twice should have no effect
      val request2 =
        console_api.UpdateGroupRequest(group1Id, Seq(contact.contactId.toString), Seq())
      val rpcRequest2 = SignedRpcRequest.generate(keyPair, did, request2)

      usingApiAs(rpcRequest2) { serviceStub =>
        serviceStub.updateGroup(request2)
        listContacts(institutionId, group1Name) must be(List(contact))
        listContacts(institutionId, group2Name) must be(List())
      }
    }

    "be able to remove contacts" in {
      val keyPair = EC.generateKeyPair()
      val publicKey = keyPair.publicKey
      val did = generateDid(publicKey)
      val institutionId = createParticipant(did)

      val groupNames = List(group1Name, group2Name)
      val List(group1Id, _) = groupNames.map { groupName =>
        institutionGroupsRepository
          .create(institutionId, groupName, Set())
          .value
          .futureValue
          .toOption
          .value
          .id
          .toString
      }
      val contact1 = DataPreparation.createContact(institutionId, groupName = Some(group1Name))
      val contact2 = DataPreparation.createContact(institutionId, groupName = Some(group2Name))

      listContacts(institutionId, group1Name) must be(List(contact1))
      listContacts(institutionId, group2Name) must be(List(contact2))

      val request1 =
        console_api.UpdateGroupRequest(group1Id, Seq(), Seq(contact1.contactId.toString))
      val rpcRequest1 = SignedRpcRequest.generate(keyPair, did, request1)

      usingApiAs(rpcRequest1) { serviceStub =>
        serviceStub.updateGroup(request1)

        listContacts(institutionId, group1Name) must be(List())
        listContacts(institutionId, group2Name) must be(List(contact2))
      }

      // Removing the same contact twice should have no effect
      val request2 =
        console_api.UpdateGroupRequest(group1Id, Seq(), Seq(contact1.contactId.toString))
      val rpcRequest2 = SignedRpcRequest.generate(keyPair, did, request2)

      usingApiAs(rpcRequest2) { serviceStub =>
        serviceStub.updateGroup(request2)

        listContacts(institutionId, group1Name) must be(List())
        listContacts(institutionId, group2Name) must be(List(contact2))
      }
    }

    "be able to add and remove at the same time" in {
      val keyPair = EC.generateKeyPair()
      val publicKey = keyPair.publicKey
      val did = generateDid(publicKey)
      val institutionId = createParticipant(did)

      val groupNames = List(group1Name, group2Name)
      val List(group1Id, _) = groupNames.map { groupName =>
        institutionGroupsRepository
          .create(institutionId, groupName, Set())
          .value
          .futureValue
          .toOption
          .value
          .id
          .toString
      }
      val contact1 = DataPreparation.createContact(institutionId, groupName = Some(group1Name))
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
      val publicKey1 = keyPair1.publicKey
      val did1 = generateDid(publicKey1)
      val institutionId1 = createParticipant(did1)
      val keyPair2 = EC.generateKeyPair()
      val publicKey2 = keyPair2.publicKey
      val did2 = generateDid(publicKey2)
      val institutionId2 = createParticipant(did2)

      val group1Id =
        institutionGroupsRepository
          .create(institutionId1, group1Name, Set())
          .value
          .futureValue
          .toOption
          .value
          .id
          .toString
      val contact = DataPreparation.createContact(institutionId2)

      val request =
        console_api.UpdateGroupRequest(group1Id, Seq(contact.contactId.toString), Seq())
      val rpcRequest = SignedRpcRequest.generate(keyPair2, did2, request)

      usingApiAs(rpcRequest) { serviceStub =>
        intercept[RuntimeException](
          serviceStub.updateGroup(request)
        )
      }
    }

    "reject requests with non-matching contact institution" in {
      val keyPair1 = EC.generateKeyPair()
      val publicKey1 = keyPair1.publicKey
      val did1 = generateDid(publicKey1)
      val institutionId1 = createParticipant(did1)
      val keyPair2 = EC.generateKeyPair()
      val publicKey2 = keyPair2.publicKey
      val did2 = generateDid(publicKey2)
      val institutionId2 = createParticipant(did2)

      val group1Id =
        institutionGroupsRepository
          .create(institutionId1, group1Name, Set())
          .value
          .futureValue
          .toOption
          .value
          .id
          .toString
      val contact = DataPreparation.createContact(institutionId2)

      val request =
        console_api.UpdateGroupRequest(group1Id, Seq(contact.contactId.toString), Seq())
      val rpcRequest = SignedRpcRequest.generate(keyPair1, did1, request)

      usingApiAs(rpcRequest) { serviceStub =>
        intercept[RuntimeException](
          serviceStub.updateGroup(request)
        )
      }
    }
  }

  private def listContacts(institutionId: ParticipantId, groupName: InstitutionGroup.Name): List[Contact] =
    institutionGroupsRepository.listContacts(institutionId, groupName).value.futureValue.toOption.value

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
}
