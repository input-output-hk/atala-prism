package io.iohk.atala.mirror.services

import io.iohk.atala.prism.repositories.PostgresRepositorySpec
import io.iohk.atala.mirror.db.ConnectionDao
import org.mockito.scalatest.MockitoSugar
import monix.execution.Scheduler.Implicits.global
import io.iohk.atala.mirror.models.Connection.ConnectionToken
import doobie.implicits._
import io.iohk.atala.mirror.models.Connection
import io.iohk.atala.mirror.protos.mirror_api.CreateAccountResponse
import io.iohk.atala.mirror.stubs.ConnectorClientServiceStub

import scala.concurrent.duration.DurationInt

// mill -i mirror.test.single io.iohk.atala.mirror.services.MirrorServiceSpec
class MirrorServiceSpec extends PostgresRepositorySpec with MockitoSugar {

  "create new account" in {
    // given
    val token = "token"
    val connectorClientStub = new ConnectorClientServiceStub(token)
    val mirrorService = new MirrorService(databaseTask, connectorClientStub)

    // when
    val connection = (for {
      response <- mirrorService.createAccount
      connection <- ConnectionDao.findBy(ConnectionToken(token)).transact(databaseTask)
    } yield response -> connection).runSyncUnsafe(1.minute)

    // then
    connection mustBe CreateAccountResponse(token) -> Some(
      Connection(
        Connection.ConnectionToken(token),
        None,
        Connection.ConnectionState.Invited
      )
    )
  }
}
