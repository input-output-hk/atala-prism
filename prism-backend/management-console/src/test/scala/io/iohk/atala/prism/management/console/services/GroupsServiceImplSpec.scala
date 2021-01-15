package io.iohk.atala.prism.management.console.services

import java.util.UUID

import cats.effect.IO
import doobie.util.transactor.Transactor
import doobie.implicits._
import io.circe.Json
import io.iohk.atala.prism.auth.SignedRpcRequest
import io.iohk.atala.prism.auth.grpc.GrpcAuthenticationHeaderParser
import io.iohk.atala.prism.crypto.EC
import io.iohk.atala.prism.identity.DID
import io.iohk.atala.prism.management.console.ManagementConsoleAuthenticator
import io.iohk.atala.prism.management.console.models.{
  Contact,
  CreateContact,
  InstitutionGroup,
  ParticipantId,
  ParticipantInfo,
  ParticipantLogo
}
import io.iohk.atala.prism.management.console.repositories.daos.ParticipantsDAO
import io.iohk.atala.prism.management.console.repositories.{
  ContactsRepository,
  InstitutionGroupsRepository,
  ParticipantsRepository,
  RequestNoncesRepository
}
import io.iohk.atala.prism.protos.cmanager_api
import io.iohk.atala.prism.{DIDGenerator, RpcSpecBase}
import org.mockito.MockitoSugar._
import org.scalatest.OptionValues._

import scala.concurrent.duration.DurationDouble

class GroupsServiceImplSpec extends RpcSpecBase with DIDGenerator {

  private implicit val pc: PatienceConfig = PatienceConfig(20.seconds, 20.millis)
  private val usingApiAs = usingApiAsConstructor(new cmanager_api.GroupsServiceGrpc.GroupsServiceBlockingStub(_, _))

  private lazy val institutionGroupsRepository = new InstitutionGroupsRepository(database)
  private lazy val participantsRepository = new ParticipantsRepository(database)
  private lazy val requestNoncesRepository = new RequestNoncesRepository.PostgresImpl(database)(executionContext)
  private lazy val contactsRepository = new ContactsRepository(database)
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
      cmanager_api.GroupsServiceGrpc
        .bindService(new GroupsServiceImpl(institutionGroupsRepository, authenticator), executionContext)
    )

  "createGroup" should {
    "create a group" in {
      val keyPair = EC.generateKeyPair()
      val publicKey = keyPair.publicKey
      val did = generateDid(publicKey)
      val institutionId = createParticipant(did)
      val newGroup = InstitutionGroup.Name("IOHK University")
      val request = cmanager_api.CreateGroupRequest(newGroup.value)
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
  }

  "getGroups" should {
    "return available groups" in {
      val keyPair = EC.generateKeyPair()
      val publicKey = keyPair.publicKey
      val did = generateDid(publicKey)
      val institutionId = createParticipant(did)

      val groups = List("Blockchain 2020", "Finance 2020").map(InstitutionGroup.Name.apply)
      groups.foreach { group =>
        institutionGroupsRepository.create(institutionId, group).value.futureValue.toOption.value
      }

      val request = cmanager_api.GetGroupsRequest()
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
        institutionGroupsRepository.create(institutionId, group).value.futureValue.toOption.value
      }
      createRandomContact(institutionId, Some(groups(0)))
      createRandomContact(institutionId, Some(groups(0)))
      createRandomContact(institutionId, Some(groups(1)))

      val request = cmanager_api.GetGroupsRequest()
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
        institutionGroupsRepository.create(issuerId, group).value.futureValue.toOption.value
      }
      createRandomContact(issuerId, Some(groups(0)))
      val contact = createRandomContact(issuerId, Some(groups(0)))
      createRandomContact(issuerId, Some(groups(1)))

      val request = cmanager_api.GetGroupsRequest().withContactId(contact.contactId.value.toString)
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

      val request = cmanager_api.GetGroupsRequest().withContactId("xyz")
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
        institutionGroupsRepository.create(institutionId, groupName).value.futureValue.toOption.value.id.value.toString
      }
      val contact = createRandomContact(institutionId)

      val request1 =
        cmanager_api.UpdateGroupRequest(group1Id, Seq(contact.contactId.value.toString), Seq())
      val rpcRequest1 = SignedRpcRequest.generate(keyPair, did, request1)

      usingApiAs(rpcRequest1) { serviceStub =>
        serviceStub.updateGroup(request1)
        listContacts(institutionId, group1Name) must be(List(contact))
        listContacts(institutionId, group2Name) must be(List())
      }

      // Adding the same contact twice should have no effect
      val request2 =
        cmanager_api.UpdateGroupRequest(group1Id, Seq(contact.contactId.value.toString), Seq())
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
        institutionGroupsRepository.create(institutionId, groupName).value.futureValue.toOption.value.id.value.toString
      }
      val contact1 = createRandomContact(institutionId, Some(group1Name))
      val contact2 = createRandomContact(institutionId, Some(group2Name))

      listContacts(institutionId, group1Name) must be(List(contact1))
      listContacts(institutionId, group2Name) must be(List(contact2))

      val request1 =
        cmanager_api.UpdateGroupRequest(group1Id, Seq(), Seq(contact1.contactId.value.toString))
      val rpcRequest1 = SignedRpcRequest.generate(keyPair, did, request1)

      usingApiAs(rpcRequest1) { serviceStub =>
        serviceStub.updateGroup(request1)

        listContacts(institutionId, group1Name) must be(List())
        listContacts(institutionId, group2Name) must be(List(contact2))
      }

      // Removing the same contact twice should have no effect
      val request2 =
        cmanager_api.UpdateGroupRequest(group1Id, Seq(), Seq(contact1.contactId.value.toString))
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
        institutionGroupsRepository.create(institutionId, groupName).value.futureValue.toOption.value.id.value.toString
      }
      val contact1 = createRandomContact(institutionId, Some(group1Name))
      val contact2 = createRandomContact(institutionId)

      listContacts(institutionId, group1Name) must be(List(contact1))
      listContacts(institutionId, group2Name) must be(List())

      val request1 =
        cmanager_api.UpdateGroupRequest(
          group1Id,
          Seq(contact2.contactId.value.toString),
          Seq(contact1.contactId.value.toString)
        )
      val rpcRequest1 = SignedRpcRequest.generate(keyPair, did, request1)

      usingApiAs(rpcRequest1) { serviceStub =>
        serviceStub.updateGroup(request1)

        listContacts(institutionId, group1Name) must be(List(contact2))
        listContacts(institutionId, group2Name) must be(List())
      }

      // Adding and removing the same contact at the same time should have no effect
      val request2 =
        cmanager_api.UpdateGroupRequest(
          group1Id,
          Seq(contact1.contactId.value.toString),
          Seq(contact1.contactId.value.toString)
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
          .create(institutionId1, group1Name)
          .value
          .futureValue
          .toOption
          .value
          .id
          .value
          .toString
      val contact = createRandomContact(institutionId2)

      val request =
        cmanager_api.UpdateGroupRequest(group1Id, Seq(contact.contactId.value.toString), Seq())
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
          .create(institutionId1, group1Name)
          .value
          .futureValue
          .toOption
          .value
          .id
          .value
          .toString
      val contact = createRandomContact(institutionId2)

      val request =
        cmanager_api.UpdateGroupRequest(group1Id, Seq(contact.contactId.value.toString), Seq())
      val rpcRequest = SignedRpcRequest.generate(keyPair1, did1, request)

      usingApiAs(rpcRequest) { serviceStub =>
        intercept[RuntimeException](
          serviceStub.updateGroup(request)
        )
      }
    }
  }

  private def createRandomContact(
      institutionId: ParticipantId,
      maybeGroupName: Option[InstitutionGroup.Name] = None
  ): Contact = {
    val contactData = CreateContact(institutionId, Contact.ExternalId.random(), Json.Null)
    contactsRepository.create(contactData, maybeGroupName).value.futureValue.toOption.value
  }

  private def listContacts(institutionId: ParticipantId, groupName: InstitutionGroup.Name): List[Contact] =
    institutionGroupsRepository.listContacts(institutionId, groupName).value.futureValue.toOption.value

  private def createParticipant(did: DID)(implicit
      database: Transactor[IO]
  ): ParticipantId = {
    val id = UUID.randomUUID()

    val participant =
      ParticipantInfo(
        ParticipantId(id),
        "",
        did,
        Some(ParticipantLogo(Vector()))
      )
    ParticipantsDAO.insert(participant).transact(database).unsafeRunSync()

    ParticipantId(id)
  }
}
