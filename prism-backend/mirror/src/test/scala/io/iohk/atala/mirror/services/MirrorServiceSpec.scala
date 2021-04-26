package io.iohk.atala.mirror.services

import java.time.Instant

import monix.eval.Task
import io.iohk.atala.prism.repositories.PostgresRepositorySpec
import io.iohk.atala.mirror.db.ConnectionDao
import org.mockito.scalatest.MockitoSugar
import monix.execution.Scheduler.Implicits.global
import io.iohk.atala.prism.models.{ConnectionState, ConnectionToken}
import doobie.implicits._
import io.iohk.atala.mirror.MirrorFixtures
import io.iohk.atala.mirror.models.Connection
import io.iohk.atala.mirror.protos.mirror_api.{CreateAccountResponse, GetCredentialForAddressRequest}
import io.iohk.atala.prism.stubs.ConnectorClientServiceStub
import org.scalatest.OptionValues._

import scala.concurrent.duration.DurationInt

// sbt "project mirror" "testOnly *services.MirrorServiceSpec"
class MirrorServiceSpec extends PostgresRepositorySpec[Task] with MockitoSugar with MirrorFixtures {
  import UserCredentialFixtures._, CardanoAddressInfoFixtures._, CredentialFixtures._

  "create new account" in {
    // given
    val token = "token"
    val connectorClientStub = new ConnectorClientServiceStub(token)
    val mirrorService = new MirrorServiceImpl(database, connectorClientStub)
    val updatedAt = Instant.now()

    // when
    val connection = (for {
      response <- mirrorService.createAccount
      connection <- ConnectionDao.findByConnectionToken(ConnectionToken(token)).transact(database)
      updatedConnection = connection.map(_.copy(updatedAt = updatedAt))
    } yield response -> updatedConnection).runSyncUnsafe(1.minute)

    // then
    connection mustBe CreateAccountResponse(token) -> Some(
      Connection(
        ConnectionToken(token),
        None,
        ConnectionState.Invited,
        updatedAt,
        None,
        None
      )
    )
  }

  "getCredentialForAddress" should {
    "return credentials for address" in new MirrorServiceFixtures {
      // given
      (for {
        _ <- ConnectionFixtures.insertAll(database)
        _ <- UserCredentialFixtures.insertAll(database)
        _ <- CardanoAddressInfoFixtures.insertAll(database)
      } yield ()).runSyncUnsafe()

      // when
      val response = mirrorService
        .getCredentialForAddress(GetCredentialForAddressRequest(cardanoAddressInfo1.cardanoAddress.value))
        .runSyncUnsafe()

      // then
      val credentials = response.getData.credentials
      credentials.size mustBe 2
      val credential = credentials.head
      credential.rawCredential mustBe userCredential1.rawCredential.rawCredential
      credential.issuersDidOption.issuersDid mustBe userCredential1.issuersDID.map(_.value)
      credential.status mustBe userCredential1.status.entryName
    }

    "return error when address cannot be found" in new MirrorServiceFixtures {
      // when
      val response = mirrorService
        .getCredentialForAddress(GetCredentialForAddressRequest(cardanoAddressInfo1.cardanoAddress.value))
        .runSyncUnsafe()

      // then
      response.getError.isAddressNotFound mustBe true
    }
  }

  "getIdentityInfoForAddress" should {
    "return ivms101 Person info for address" in new MirrorServiceFixtures {
      // given
      (for {
        _ <- ConnectionFixtures.insertAll(database)
        _ <- UserCredentialFixtures.insertAll(database)
        _ <- CardanoAddressInfoFixtures.insertAll(database)
      } yield ()).runSyncUnsafe()

      // when
      val response = mirrorService
        .getIdentityInfoForAddress(cardanoAddressInfo1.cardanoAddress)
        .runSyncUnsafe()

      // then
      val person = response.value
      val naturalPerson = person.getNaturalPerson
      naturalPerson.name.flatMap(_.nameIdentifiers.headOption).map(_.primaryIdentifier) mustBe Some(
        redlandIdCredential2.name
      )
      naturalPerson.nationalIdentification.map(_.nationalIdentifier) mustBe Some(redlandIdCredential2.identityNumber)
      naturalPerson.dateAndPlaceOfBirth.map(_.dateOfBirth) mustBe Some(redlandIdCredential2.dateOfBirth)
    }

    "return error when address cannot be found" in new MirrorServiceFixtures {
      // when
      val response = mirrorService
        .getIdentityInfoForAddress(cardanoAddressInfo1.cardanoAddress)
        .runSyncUnsafe()

      // then
      response mustBe None
    }
  }

  trait MirrorServiceFixtures {
    val connectorClientStub = new ConnectorClientServiceStub()
    val mirrorService = new MirrorServiceImpl(database, connectorClientStub)
  }
}
