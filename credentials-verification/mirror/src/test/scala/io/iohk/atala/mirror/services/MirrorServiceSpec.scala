package io.iohk.atala.mirror.services

import io.iohk.atala.prism.repositories.PostgresRepositorySpec
import io.iohk.atala.mirror.db.ConnectionDao
import org.mockito.scalatest.MockitoSugar
import monix.execution.Scheduler.Implicits.global
import io.iohk.atala.mirror.models.Connection.ConnectionToken
import doobie.implicits._
import io.iohk.atala.mirror.MirrorFixtures
import io.iohk.atala.mirror.models.Connection
import io.iohk.atala.mirror.protos.mirror_api.{CreateAccountResponse, GetCredentialForAddressRequest}
import io.iohk.atala.mirror.stubs.ConnectorClientServiceStub

import scala.concurrent.duration.DurationInt

// sbt "project mirror" "testOnly *services.MirrorServiceSpec"
class MirrorServiceSpec extends PostgresRepositorySpec with MockitoSugar with MirrorFixtures {
  import UserCredentialFixtures._, CardanoAddressInfoFixtures._

  "create new account" in {
    // given
    val token = "token"
    val connectorClientStub = new ConnectorClientServiceStub(token)
    val mirrorService = new MirrorService(databaseTask, connectorClientStub)

    // when
    val connection = (for {
      response <- mirrorService.createAccount
      connection <- ConnectionDao.findByConnectionToken(ConnectionToken(token)).transact(databaseTask)
    } yield response -> connection).runSyncUnsafe(1.minute)

    // then
    connection mustBe CreateAccountResponse(token) -> Some(
      Connection(
        Connection.ConnectionToken(token),
        None,
        Connection.ConnectionState.Invited,
        None
      )
    )
  }

  "getCredentialForAddress" should {
    "return credentials for address" in new MirrorServiceFixtures {
      // given
      (for {
        _ <- ConnectionFixtures.insertAll(databaseTask)
        _ <- UserCredentialFixtures.insertAll(databaseTask)
        _ <- CardanoAddressInfoFixtures.insertAll(databaseTask)
      } yield ()).runSyncUnsafe()

      // when
      val response = mirrorService
        .getCredentialForAddress(GetCredentialForAddressRequest(cardanoAddressInfo1.cardanoAddress.address))
        .runSyncUnsafe()

      // then
      val credentials = response.getData.credentials
      credentials.size mustBe 1
      val credential = credentials.head
      credential.rawCredential mustBe userCredential1.rawCredential.rawCredential
      credential.issuersDidOption.issuersDid mustBe userCredential1.issuersDID.map(_.value)
      credential.status mustBe userCredential1.status.entryName
    }

    "return error when address cannot be found" in new MirrorServiceFixtures {
      // when
      val response = mirrorService
        .getCredentialForAddress(GetCredentialForAddressRequest(cardanoAddressInfo1.cardanoAddress.address))
        .runSyncUnsafe()

      // then
      response.getError.isAddressNotFound mustBe true
    }
  }

  trait MirrorServiceFixtures {
    val connectorClientStub = new ConnectorClientServiceStub()
    val mirrorService = new MirrorService(databaseTask, connectorClientStub)
  }
}
