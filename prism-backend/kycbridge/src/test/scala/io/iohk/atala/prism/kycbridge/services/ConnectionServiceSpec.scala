package io.iohk.atala.prism.kycbridge.services

import monix.eval.Task
import io.iohk.atala.prism.identity.DID
import io.iohk.atala.prism.kycbridge.KycBridgeFixtures
import io.iohk.atala.prism.kycbridge.db.ConnectionDao
import io.iohk.atala.prism.models.{ConnectionId, ConnectionState, ConnectionToken}
import io.iohk.atala.prism.protos.connector_models.ConnectionInfo
import io.iohk.atala.prism.repositories.PostgresRepositorySpec
import io.iohk.atala.prism.stubs.{ConnectorClientServiceStub, ProcessingTaskServiceStub}
import org.mockito.scalatest.MockitoSugar
import doobie.implicits._
import io.iohk.atala.prism.kycbridge.task.lease.system.KycBridgeProcessingTaskState
import monix.execution.Scheduler.Implicits.global

import scala.concurrent.duration.DurationInt

// sbt "project kycbridge" "testOnly *services.ConnectionServiceSpec"
class ConnectionServiceSpec extends PostgresRepositorySpec[Task] with MockitoSugar with KycBridgeFixtures {
  import ConnectionFixtures._

  "updateCredentialsStream" should {
    "update connections periodically" in {
      // given
      val connectionId = ConnectionId.random()
      val token = connection1.token.token
      val participantDID = DID.buildPrismDID("did1")
      val connectionInfos =
        Seq(ConnectionInfo(token = token, connectionId = connectionId.toString, participantDid = participantDID.value))

      val connectorClientStub = new ConnectorClientServiceStub(connectionInfos = connectionInfos)
      val processingTaskService = new ProcessingTaskServiceStub[KycBridgeProcessingTaskState]()
      val connectionService = new ConnectionService(database, connectorClientStub, processingTaskService)

      // when
      val result = (for {
        _ <- ConnectionDao.insert(connection1).transact(database)
        _ <-
          connectionService.connectionUpdateStream
            .interruptAfter(1.seconds)
            .compile
            .drain
        result <- ConnectionDao.findByConnectionToken(ConnectionToken(token)).transact(database)
      } yield result).runSyncUnsafe(1.minute)

      // then
      result.map(_.copy(updatedAt = connection1.updatedAt)) mustBe Some(
        connection1.copy(
          id = Some(connectionId),
          state = ConnectionState.Connected
        )
      )
      processingTaskService.createTaskInvokeCount.get() mustBe 1
    }
  }
}
