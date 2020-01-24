package io.iohk.cvp.cmanager.grpc.services

import io.iohk.connector.RpcSpecBase
import io.iohk.cvp.cmanager.models.Issuer
import io.iohk.cvp.cmanager.protos
import io.iohk.cvp.cmanager.protos.GroupsServiceGrpc
import io.iohk.cvp.cmanager.repositories.{IssuerGroupsRepository, IssuersRepository}
import io.iohk.cvp.models.ParticipantId
import org.scalatest.EitherValues._

import scala.concurrent.duration.DurationDouble

class GroupsServiceSpec extends RpcSpecBase {

  override val tables = List("issuer_groups", "credentials", "students", "issuers")

  private implicit val executionContext = scala.concurrent.ExecutionContext.global
  private implicit val pc: PatienceConfig = PatienceConfig(20.seconds, 20.millis)
  private val usingApiAs = usingApiAsConstructor(new GroupsServiceGrpc.GroupsServiceBlockingStub(_, _))

  private lazy val issuerGroupsRepository = new IssuerGroupsRepository(database)
  private lazy val issuersRepository = new IssuersRepository(database)

  override def services = Seq(
    GroupsServiceGrpc
      .bindService(new GroupsServiceImpl(issuerGroupsRepository), executionContext)
  )

  "createGroup" should {
    "create a group" in {
      val issuerId = issuersRepository
        .insert(IssuersRepository.IssuerCreationData(Issuer.Name("IOHK"), "did:prism:issuer1", None))
        .value
        .futureValue
        .right
        .value

      usingApiAs(toParticipantId(issuerId)) { serviceStub =>
        val newGroup = "IOHK University"
        val request = protos.CreateGroupRequest(newGroup)
        val _ = serviceStub.createGroup(request)

        // the new group needs to exist
        val groups = issuerGroupsRepository.getBy(issuerId).value.futureValue.right.value
        groups must contain(newGroup)
      }
    }
  }

  "getGroups" should {
    "return available groups" in {
      val issuerId = issuersRepository
        .insert(IssuersRepository.IssuerCreationData(Issuer.Name("IOHK"), "did:prism:issuer1", None))
        .value
        .futureValue
        .right
        .value

      val groups = List("Blockchain 2020", "Finance 2020")
      groups.foreach { group =>
        issuerGroupsRepository.create(issuerId, group).value.futureValue.right.value
      }

      usingApiAs(toParticipantId(issuerId)) { serviceStub =>
        val request = protos.GetGroupsRequest()
        val result = serviceStub.getGroups(request)
        result.groups.map(_.name) must be(groups)
      }
    }
  }

  private def toParticipantId(issuer: Issuer.Id): ParticipantId = {
    ParticipantId(issuer.value)
  }
}
