package io.iohk.atala.prism.console.services

import java.util.UUID

import cats.effect.IO
import doobie.implicits._
import doobie.util.transactor.Transactor
import io.circe.Json
import io.iohk.atala.prism.{DIDGenerator, RpcSpecBase}
import io.iohk.atala.prism.auth.SignedRpcRequest
import io.iohk.atala.prism.auth.grpc.GrpcAuthenticationHeaderParser
import io.iohk.atala.prism.connector.model.{ParticipantInfo, ParticipantLogo, ParticipantType}
import io.iohk.atala.prism.connector.repositories.daos.ParticipantsDAO
import io.iohk.atala.prism.connector.repositories.{ParticipantsRepository, RequestNoncesRepository}
import io.iohk.atala.prism.connector.ConnectorAuthenticator
import io.iohk.atala.prism.console.models.{Contact, CreateContact, Institution, IssuerGroup}
import io.iohk.atala.prism.console.repositories.{ContactsRepository, GroupsRepository}
import io.iohk.atala.prism.crypto.{EC, ECPublicKey, SHA256Digest}
import io.iohk.atala.prism.models.{ParticipantId, TransactionId}
import io.iohk.atala.prism.protos.cmanager_api
import org.mockito.MockitoSugar._
import org.scalatest.OptionValues._

import scala.concurrent.duration.DurationDouble

class GroupsServiceImplSpec extends RpcSpecBase with DIDGenerator {

  private implicit val executionContext = scala.concurrent.ExecutionContext.global
  private implicit val pc: PatienceConfig = PatienceConfig(20.seconds, 20.millis)
  private val usingApiAs = usingApiAsConstructor(new cmanager_api.GroupsServiceGrpc.GroupsServiceBlockingStub(_, _))

  private lazy val issuerGroupsRepository = new GroupsRepository(database)
  private lazy val participantsRepository = new ParticipantsRepository(database)
  private lazy val requestNoncesRepository = new RequestNoncesRepository.PostgresImpl(database)(executionContext)
  private lazy val contactsRepository = new ContactsRepository(database)
  protected lazy val nodeMock = mock[io.iohk.atala.prism.protos.node_api.NodeServiceGrpc.NodeService]
  private lazy val authenticator =
    new ConnectorAuthenticator(
      participantsRepository,
      requestNoncesRepository,
      nodeMock,
      GrpcAuthenticationHeaderParser
    )

  override def services =
    Seq(
      cmanager_api.GroupsServiceGrpc
        .bindService(new GroupsServiceImpl(issuerGroupsRepository, authenticator), executionContext)
    )

  "createGroup" should {
    "create a group" in {
      val keyPair = EC.generateKeyPair()
      val publicKey = keyPair.publicKey
      val did = generateDid(publicKey)
      val issuerId = createIssuer(publicKey, did)
      val newGroup = IssuerGroup.Name("IOHK University")
      val request = cmanager_api.CreateGroupRequest(newGroup.value)
      val rpcRequest = SignedRpcRequest.generate(keyPair, did, request)

      usingApiAs(rpcRequest) { serviceStub =>
        val _ = serviceStub.createGroup(request)

        // the new group needs to exist
        val groups = issuerGroupsRepository.getBy(issuerId).value.futureValue.toOption.value
        groups.map(_.value.name) must contain(newGroup)
      }
    }
  }

  "getGroups" should {
    "return available groups" in {
      val keyPair = EC.generateKeyPair()
      val publicKey = keyPair.publicKey
      val did = generateDid(publicKey)
      val issuerId = createIssuer(publicKey, did)

      val groups = List("Blockchain 2020", "Finance 2020").map(IssuerGroup.Name.apply)
      groups.foreach { group =>
        issuerGroupsRepository.create(issuerId, group).value.futureValue.toOption.value
      }

      val request = cmanager_api.GetGroupsRequest()
      val rpcRequest = SignedRpcRequest.generate(keyPair, did, request)

      usingApiAs(rpcRequest) { serviceStub =>
        val result = serviceStub.getGroups(request)
        result.groups.map(_.name).map(IssuerGroup.Name.apply) must be(groups)
      }
    }

    "return the contact count on each group" in {
      val keyPair = EC.generateKeyPair()
      val publicKey = keyPair.publicKey
      val did = generateDid(publicKey)
      val issuerId = createIssuer(publicKey, did)

      val groups = List("Blockchain 2020", "Finance 2020").map(IssuerGroup.Name.apply)
      groups.foreach { group =>
        issuerGroupsRepository.create(issuerId, group).value.futureValue.toOption.value
      }
      createRandomContact(issuerId, Some(groups(0)))
      createRandomContact(issuerId, Some(groups(0)))
      createRandomContact(issuerId, Some(groups(1)))

      val request = cmanager_api.GetGroupsRequest()
      val rpcRequest = SignedRpcRequest.generate(keyPair, did, request)

      usingApiAs(rpcRequest) { serviceStub =>
        val result = serviceStub.getGroups(request).groups

        result.map(_.numberOfContacts) must be(List(2, 1))
      }
    }
  }

  "updateGroup" should {
    val group1 = "Group 1"
    val group1Name = IssuerGroup.Name(group1)
    val group2 = "Group 2"
    val group2Name = IssuerGroup.Name(group2)

    "be able to add new contacts" in {
      val keyPair = EC.generateKeyPair()
      val publicKey = keyPair.publicKey
      val did = generateDid(publicKey)
      val issuerId = createIssuer(publicKey, did)

      val groupNames = List(group1Name, group2Name)
      val List(group1Id, _) = groupNames.map { groupName =>
        issuerGroupsRepository.create(issuerId, groupName).value.futureValue.toOption.value.id.value.toString
      }
      val contact = createRandomContact(issuerId)

      val request1 =
        cmanager_api.UpdateGroupRequest(group1Id, Seq(contact.contactId.value.toString), Seq())
      val rpcRequest1 = SignedRpcRequest.generate(keyPair, did, request1)

      usingApiAs(rpcRequest1) { serviceStub =>
        serviceStub.updateGroup(request1)
        listContacts(issuerId, group1Name) must be(List(contact))
        listContacts(issuerId, group2Name) must be(List())
      }

      // Adding the same contact twice should have no effect
      val request2 =
        cmanager_api.UpdateGroupRequest(group1Id, Seq(contact.contactId.value.toString), Seq())
      val rpcRequest2 = SignedRpcRequest.generate(keyPair, did, request2)

      usingApiAs(rpcRequest2) { serviceStub =>
        serviceStub.updateGroup(request2)
        listContacts(issuerId, group1Name) must be(List(contact))
        listContacts(issuerId, group2Name) must be(List())
      }
    }

    "be able to remove contacts" in {
      val keyPair = EC.generateKeyPair()
      val publicKey = keyPair.publicKey
      val did = generateDid(publicKey)
      val issuerId = createIssuer(publicKey, did)

      val groupNames = List(group1Name, group2Name)
      val List(group1Id, _) = groupNames.map { groupName =>
        issuerGroupsRepository.create(issuerId, groupName).value.futureValue.toOption.value.id.value.toString
      }
      val contact1 = createRandomContact(issuerId, Some(group1Name))
      val contact2 = createRandomContact(issuerId, Some(group2Name))

      listContacts(issuerId, group1Name) must be(List(contact1))
      listContacts(issuerId, group2Name) must be(List(contact2))

      val request1 =
        cmanager_api.UpdateGroupRequest(group1Id, Seq(), Seq(contact1.contactId.value.toString))
      val rpcRequest1 = SignedRpcRequest.generate(keyPair, did, request1)

      usingApiAs(rpcRequest1) { serviceStub =>
        serviceStub.updateGroup(request1)

        listContacts(issuerId, group1Name) must be(List())
        listContacts(issuerId, group2Name) must be(List(contact2))
      }

      // Removing the same contact twice should have no effect
      val request2 =
        cmanager_api.UpdateGroupRequest(group1Id, Seq(), Seq(contact1.contactId.value.toString))
      val rpcRequest2 = SignedRpcRequest.generate(keyPair, did, request2)

      usingApiAs(rpcRequest2) { serviceStub =>
        serviceStub.updateGroup(request2)

        listContacts(issuerId, group1Name) must be(List())
        listContacts(issuerId, group2Name) must be(List(contact2))
      }
    }

    "be able to add and remove at the same time" in {
      val keyPair = EC.generateKeyPair()
      val publicKey = keyPair.publicKey
      val did = generateDid(publicKey)
      val issuerId = createIssuer(publicKey, did)

      val groupNames = List(group1Name, group2Name)
      val List(group1Id, _) = groupNames.map { groupName =>
        issuerGroupsRepository.create(issuerId, groupName).value.futureValue.toOption.value.id.value.toString
      }
      val contact1 = createRandomContact(issuerId, Some(group1Name))
      val contact2 = createRandomContact(issuerId)

      listContacts(issuerId, group1Name) must be(List(contact1))
      listContacts(issuerId, group2Name) must be(List())

      val request1 =
        cmanager_api.UpdateGroupRequest(
          group1Id,
          Seq(contact2.contactId.value.toString),
          Seq(contact1.contactId.value.toString)
        )
      val rpcRequest1 = SignedRpcRequest.generate(keyPair, did, request1)

      usingApiAs(rpcRequest1) { serviceStub =>
        serviceStub.updateGroup(request1)

        listContacts(issuerId, group1Name) must be(List(contact2))
        listContacts(issuerId, group2Name) must be(List())
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

        listContacts(issuerId, group1Name) must be(List(contact2))
        listContacts(issuerId, group2Name) must be(List())
      }
    }

    "reject requests with non-matching group issuer" in {
      val keyPair1 = EC.generateKeyPair()
      val publicKey1 = keyPair1.publicKey
      val did1 = generateDid(publicKey1)
      val issuerId1 = createIssuer(publicKey1, did1)
      val keyPair2 = EC.generateKeyPair()
      val publicKey2 = keyPair2.publicKey
      val did2 = generateDid(publicKey2)
      val issuerId2 = createIssuer(publicKey2, did2)

      val group1Id =
        issuerGroupsRepository.create(issuerId1, group1Name).value.futureValue.toOption.value.id.value.toString
      val contact = createRandomContact(issuerId2)

      val request =
        cmanager_api.UpdateGroupRequest(group1Id, Seq(contact.contactId.value.toString), Seq())
      val rpcRequest = SignedRpcRequest.generate(keyPair2, did2, request)

      usingApiAs(rpcRequest) { serviceStub =>
        intercept[RuntimeException](
          serviceStub.updateGroup(request)
        )
      }
    }

    "reject requests with non-matching contact issuer" in {
      val keyPair1 = EC.generateKeyPair()
      val publicKey1 = keyPair1.publicKey
      val did1 = generateDid(publicKey1)
      val issuerId1 = createIssuer(publicKey1, did1)
      val keyPair2 = EC.generateKeyPair()
      val publicKey2 = keyPair2.publicKey
      val did2 = generateDid(publicKey2)
      val issuerId2 = createIssuer(publicKey2, did2)

      val group1Id =
        issuerGroupsRepository.create(issuerId1, group1Name).value.futureValue.toOption.value.id.value.toString
      val contact = createRandomContact(issuerId2)

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
      issuer: Institution.Id,
      maybeGroupName: Option[IssuerGroup.Name] = None
  ): Contact = {
    val contactData = CreateContact(issuer, Contact.ExternalId.random(), Json.Null)
    contactsRepository.create(contactData, maybeGroupName).value.futureValue.toOption.value
  }

  private def listContacts(issuer: Institution.Id, groupName: IssuerGroup.Name): List[Contact] =
    issuerGroupsRepository.listContacts(issuer, groupName).value.futureValue.toOption.value

  private def createIssuer(publicKey: ECPublicKey, did: String)(implicit
      database: Transactor[IO]
  ): Institution.Id = {
    val id = UUID.randomUUID()
    val mockTransactionId =
      TransactionId.from(SHA256Digest.compute("id".getBytes).value).value

    val participant =
      ParticipantInfo(
        ParticipantId(id),
        ParticipantType.Issuer,
        Some(publicKey),
        "",
        Some(did),
        Some(ParticipantLogo(Vector())),
        Some(mockTransactionId),
        None
      )
    ParticipantsDAO.insert(participant).transact(database).unsafeRunSync()

    Institution.Id(id)
  }
}
