package io.iohk.atala.prism.cmanager.grpc.services

import java.util.UUID

import io.iohk.atala.prism.connector.model.{ParticipantLogo, ParticipantType}
import io.iohk.atala.prism.connector.repositories.ParticipantsRepository.CreateParticipantRequest
import io.iohk.atala.prism.connector.repositories.{ParticipantsRepository, RequestNoncesRepository}
import io.iohk.atala.prism.connector.{RpcSpecBase, SignedRequestsAuthenticator}
import io.iohk.atala.prism.cmanager.models.{Issuer, IssuerGroup}
import io.iohk.atala.prism.cmanager.repositories.IssuerGroupsRepository
import io.iohk.atala.prism.grpc.GrpcAuthenticationHeaderParser
import io.iohk.atala.prism.models.ParticipantId
import io.iohk.prism.protos.cmanager_api
import org.mockito.MockitoSugar._
import org.scalatest.EitherValues._

import scala.concurrent.duration.DurationDouble

class GroupsServiceImplSpec extends RpcSpecBase {

  private implicit val executionContext = scala.concurrent.ExecutionContext.global
  private implicit val pc: PatienceConfig = PatienceConfig(20.seconds, 20.millis)
  private val usingApiAs = usingApiAsConstructor(new cmanager_api.GroupsServiceGrpc.GroupsServiceBlockingStub(_, _))

  private lazy val issuerGroupsRepository = new IssuerGroupsRepository(database)
  private lazy val participantsRepository = new ParticipantsRepository(database)
  private lazy val requestNoncesRepository = new RequestNoncesRepository.PostgresImpl(database)(executionContext)
  private lazy val nodeMock = mock[io.iohk.prism.protos.node_api.NodeServiceGrpc.NodeService]
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

      usingApiAs(toParticipantId(issuerId)) { serviceStub =>
        val newGroup = IssuerGroup.Name("IOHK University")
        val request = cmanager_api.CreateGroupRequest(newGroup.value)
        val _ = serviceStub.createGroup(request)

        // the new group needs to exist
        val groups = issuerGroupsRepository.getBy(issuerId).value.futureValue.right.value
        groups must contain(newGroup)
      }
    }
  }

  "getGroups" should {
    "return available groups" in {
      val issuerId = createIssuer()

      val groups = List("Blockchain 2020", "Finance 2020").map(IssuerGroup.Name.apply)
      groups.foreach { group =>
        issuerGroupsRepository.create(issuerId, group).value.futureValue.right.value
      }

      usingApiAs(toParticipantId(issuerId)) { serviceStub =>
        val request = cmanager_api.GetGroupsRequest()
        val result = serviceStub.getGroups(request)
        result.groups.map(_.name).map(IssuerGroup.Name.apply) must be(groups)
      }
    }
  }

  private def createIssuer(): Issuer.Id = {
    val id = UUID.randomUUID()
    val mockDID = "did:prims:test"
    participantsRepository
      .create(
        CreateParticipantRequest(ParticipantId(id), ParticipantType.Issuer, "", mockDID, ParticipantLogo(Vector()))
      )
      .value
      .futureValue
    Issuer.Id(id)
  }

  private def toParticipantId(issuer: Issuer.Id): ParticipantId = {
    ParticipantId(issuer.value)
  }
}
