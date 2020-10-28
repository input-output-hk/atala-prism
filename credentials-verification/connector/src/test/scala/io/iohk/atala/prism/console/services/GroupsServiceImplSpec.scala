package io.iohk.atala.prism.console.services

import java.util.UUID

import io.circe.Json
import io.iohk.atala.prism.connector.model.{ParticipantLogo, ParticipantType}
import io.iohk.atala.prism.connector.repositories.ParticipantsRepository.CreateParticipantRequest
import io.iohk.atala.prism.connector.repositories.{ParticipantsRepository, RequestNoncesRepository}
import io.iohk.atala.prism.connector.{RpcSpecBase, SignedRequestsAuthenticator}
import io.iohk.atala.prism.console.models.{Contact, CreateContact, Institution, IssuerGroup}
import io.iohk.atala.prism.console.repositories.{ContactsRepository, GroupsRepository}
import io.iohk.atala.prism.crypto.SHA256Digest
import io.iohk.atala.prism.grpc.GrpcAuthenticationHeaderParser
import io.iohk.atala.prism.models.{Ledger, ParticipantId, TransactionId, TransactionInfo}
import io.iohk.atala.prism.protos.cmanager_api
import org.mockito.MockitoSugar._
import org.scalatest.OptionValues._

import scala.concurrent.duration.DurationDouble

class GroupsServiceImplSpec extends RpcSpecBase {

  private implicit val executionContext = scala.concurrent.ExecutionContext.global
  private implicit val pc: PatienceConfig = PatienceConfig(20.seconds, 20.millis)
  private val usingApiAs = usingApiAsConstructor(new cmanager_api.GroupsServiceGrpc.GroupsServiceBlockingStub(_, _))

  private lazy val issuerGroupsRepository = new GroupsRepository(database)
  private lazy val participantsRepository = new ParticipantsRepository(database)
  private lazy val requestNoncesRepository = new RequestNoncesRepository.PostgresImpl(database)(executionContext)
  private lazy val contactsRepository = new ContactsRepository(database)
  private lazy val nodeMock = mock[io.iohk.atala.prism.protos.node_api.NodeServiceGrpc.NodeService]
  private lazy val authenticator =
    new SignedRequestsAuthenticator(
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
      val issuerId = createIssuer()

      usingApiAs(ParticipantId(issuerId.value)) { serviceStub =>
        val newGroup = IssuerGroup.Name("IOHK University")
        val request = cmanager_api.CreateGroupRequest(newGroup.value)
        val _ = serviceStub.createGroup(request)

        // the new group needs to exist
        val groups = issuerGroupsRepository.getBy(issuerId).value.futureValue.toOption.value
        groups must contain(newGroup)
      }
    }
  }

  "getGroups" should {
    "return available groups" in {
      val issuerId = createIssuer()

      val groups = List("Blockchain 2020", "Finance 2020").map(IssuerGroup.Name.apply)
      groups.foreach { group =>
        issuerGroupsRepository.create(issuerId, group).value.futureValue.toOption.value
      }

      usingApiAs(ParticipantId(issuerId.value)) { serviceStub =>
        val request = cmanager_api.GetGroupsRequest()
        val result = serviceStub.getGroups(request)
        result.groups.map(_.name).map(IssuerGroup.Name.apply) must be(groups)
      }
    }
  }

  "updateGroup" should {
    val group1 = "Group 1"
    val group1Name = IssuerGroup.Name(group1)
    val group2 = "Group 2"
    val group2Name = IssuerGroup.Name(group2)

    "be able to add new contacts" in {
      val issuerId = createIssuer()

      val groupNames = List(group1Name, group2Name)
      val List(group1Id, _) = groupNames.map { groupName =>
        issuerGroupsRepository.create(issuerId, groupName).value.futureValue.toOption.value.id.value.toString
      }
      val contact = createRandomContact(issuerId)

      usingApiAs(ParticipantId(issuerId.value)) { serviceStub =>
        val request1 =
          cmanager_api.UpdateGroupRequest(group1Id, Seq(contact.contactId.value.toString), Seq())
        serviceStub.updateGroup(request1)

        listContacts(issuerId, group1Name) must be(List(contact))
        listContacts(issuerId, group2Name) must be(List())

        // Adding the same contact twice should have no effect
        val request2 =
          cmanager_api.UpdateGroupRequest(group1Id, Seq(contact.contactId.value.toString), Seq())
        serviceStub.updateGroup(request2)

        listContacts(issuerId, group1Name) must be(List(contact))
        listContacts(issuerId, group2Name) must be(List())
      }
    }

    "be able to remove contacts" in {
      val issuerId = createIssuer()

      val groupNames = List(group1Name, group2Name)
      val List(group1Id, _) = groupNames.map { groupName =>
        issuerGroupsRepository.create(issuerId, groupName).value.futureValue.toOption.value.id.value.toString
      }
      val contact1 = createRandomContact(issuerId, Some(group1Name))
      val contact2 = createRandomContact(issuerId, Some(group2Name))

      listContacts(issuerId, group1Name) must be(List(contact1))
      listContacts(issuerId, group2Name) must be(List(contact2))

      usingApiAs(ParticipantId(issuerId.value)) { serviceStub =>
        val request1 =
          cmanager_api.UpdateGroupRequest(group1Id, Seq(), Seq(contact1.contactId.value.toString))
        serviceStub.updateGroup(request1)

        listContacts(issuerId, group1Name) must be(List())
        listContacts(issuerId, group2Name) must be(List(contact2))

        // Removing the same contact twice should have no effect
        val request2 =
          cmanager_api.UpdateGroupRequest(group1Id, Seq(), Seq(contact1.contactId.value.toString))
        serviceStub.updateGroup(request2)

        listContacts(issuerId, group1Name) must be(List())
        listContacts(issuerId, group2Name) must be(List(contact2))
      }
    }

    "be able to add and remove at the same time" in {
      val issuerId = createIssuer()

      val groupNames = List(group1Name, group2Name)
      val List(group1Id, _) = groupNames.map { groupName =>
        issuerGroupsRepository.create(issuerId, groupName).value.futureValue.toOption.value.id.value.toString
      }
      val contact1 = createRandomContact(issuerId, Some(group1Name))
      val contact2 = createRandomContact(issuerId)

      listContacts(issuerId, group1Name) must be(List(contact1))
      listContacts(issuerId, group2Name) must be(List())

      usingApiAs(ParticipantId(issuerId.value)) { serviceStub =>
        val request1 =
          cmanager_api.UpdateGroupRequest(
            group1Id,
            Seq(contact2.contactId.value.toString),
            Seq(contact1.contactId.value.toString)
          )
        serviceStub.updateGroup(request1)

        listContacts(issuerId, group1Name) must be(List(contact2))
        listContacts(issuerId, group2Name) must be(List())

        // Adding and removing the same contact at the same time should have no effect
        val request2 =
          cmanager_api.UpdateGroupRequest(
            group1Id,
            Seq(contact1.contactId.value.toString),
            Seq(contact1.contactId.value.toString)
          )
        serviceStub.updateGroup(request2)

        listContacts(issuerId, group1Name) must be(List(contact2))
        listContacts(issuerId, group2Name) must be(List())
      }
    }

    "reject requests with non-matching group issuer" in {
      val issuerId1 = createIssuer()
      val issuerId2 = createIssuer()

      val group1Id =
        issuerGroupsRepository.create(issuerId1, group1Name).value.futureValue.toOption.value.id.value.toString
      val contact = createRandomContact(issuerId2)

      usingApiAs(ParticipantId(issuerId2.value)) { serviceStub =>
        val request =
          cmanager_api.UpdateGroupRequest(group1Id, Seq(contact.contactId.value.toString), Seq())

        intercept[RuntimeException](
          serviceStub.updateGroup(request)
        )
      }
    }

    "reject requests with non-matching contact issuer" in {
      val issuerId1 = createIssuer()
      val issuerId2 = createIssuer()

      val group1Id =
        issuerGroupsRepository.create(issuerId1, group1Name).value.futureValue.toOption.value.id.value.toString
      val contact = createRandomContact(issuerId2)

      usingApiAs(ParticipantId(issuerId1.value)) { serviceStub =>
        val request =
          cmanager_api.UpdateGroupRequest(group1Id, Seq(contact.contactId.value.toString), Seq())

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

  private def createIssuer(): Institution.Id = {
    val id = UUID.randomUUID()
    val mockDID = "did:prims:test"
    val mockTransactionInfo =
      TransactionInfo(TransactionId.from(SHA256Digest.compute("id".getBytes).value).value, Ledger.InMemory)
    participantsRepository
      .create(
        CreateParticipantRequest(
          ParticipantId(id),
          ParticipantType.Issuer,
          "",
          mockDID,
          ParticipantLogo(Vector()),
          mockTransactionInfo
        )
      )
      .value
      .futureValue
    Institution.Id(id)
  }
}
