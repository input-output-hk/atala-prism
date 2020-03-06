package io.iohk.cvp.cmanager.grpc.services

import io.iohk.connector.{RpcSpecBase, SignedRequestsAuthenticator}
import io.iohk.connector.repositories.{ConnectionsRepository, RequestNoncesRepository}
import io.iohk.cvp.cmanager.models.{Issuer, IssuerGroup}
import io.iohk.cvp.cmanager.protos
import io.iohk.cvp.cmanager.protos.GroupsServiceGrpc
import io.iohk.cvp.cmanager.repositories.{IssuerGroupsRepository, IssuersRepository}
import io.iohk.cvp.grpc.GrpcAuthenticationHeaderParser
import io.iohk.cvp.models.ParticipantId
import org.mockito.MockitoSugar._
import org.scalatest.EitherValues._

import scala.concurrent.duration.DurationDouble

class GroupsServiceImplSpec extends RpcSpecBase {

  override val tables = List("credentials", "students", "issuer_groups", "issuers", "connections")

  private implicit val executionContext = scala.concurrent.ExecutionContext.global
  private implicit val pc: PatienceConfig = PatienceConfig(20.seconds, 20.millis)
  private val usingApiAs = usingApiAsConstructor(new GroupsServiceGrpc.GroupsServiceBlockingStub(_, _))

  private lazy val issuerGroupsRepository = new IssuerGroupsRepository(database)
  private lazy val issuersRepository = new IssuersRepository(database)
  private lazy val connectionsRepository = new ConnectionsRepository.PostgresImpl(database)(executionContext)
  private lazy val requestNoncesRepository = new RequestNoncesRepository.PostgresImpl(database)(executionContext)
  private lazy val nodeMock = mock[io.iohk.prism.protos.node_api.NodeServiceGrpc.NodeService]
  private lazy val authenticator =
    new SignedRequestsAuthenticator(
      connectionsRepository,
      requestNoncesRepository,
      nodeMock,
      GrpcAuthenticationHeaderParser
    )

  override def services = Seq(
    GroupsServiceGrpc
      .bindService(new GroupsServiceImpl(issuerGroupsRepository, authenticator), executionContext)
  )

  "createGroup" should {
    "create a group" in {
      val issuerId = createIssuer()

      usingApiAs(toParticipantId(issuerId)) { serviceStub =>
        val newGroup = IssuerGroup.Name("IOHK University")
        val request = protos.CreateGroupRequest(newGroup.value)
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
        val request = protos.GetGroupsRequest()
        val result = serviceStub.getGroups(request)
        result.groups.map(_.name).map(IssuerGroup.Name.apply) must be(groups)
      }
    }
  }

  private def createIssuer(): Issuer.Id = {
    issuersRepository
      .insert(IssuersRepository.IssuerCreationData(Issuer.Name("IOHK"), "did:prism:issuer1", None))
      .value
      .futureValue
      .right
      .value
  }

  private def toParticipantId(issuer: Issuer.Id): ParticipantId = {
    ParticipantId(issuer.value)
  }
}
