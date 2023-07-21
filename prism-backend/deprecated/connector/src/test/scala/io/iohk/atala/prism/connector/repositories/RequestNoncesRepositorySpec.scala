package io.iohk.atala.prism.connector.repositories

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import doobie.implicits._
import io.iohk.atala.prism.auth.model.RequestNonce
import io.iohk.atala.prism.connector.model.ParticipantType
import io.iohk.atala.prism.connector.repositories.daos.RequestNoncesDAO
import io.iohk.atala.prism.connector.DataPreparation
import io.iohk.atala.prism.models.ParticipantId
import org.scalatest.OptionValues._
import tofu.logging.Logs

import scala.util.Try

class RequestNoncesRepositorySpec extends ConnectorRepositorySpecBase {

  val logs = Logs.sync[IO, IO]

  lazy val requestNoncesRepository =
    RequestNoncesRepository(database, logs).unsafeRunSync()

  private def available(
      participantId: ParticipantId,
      requestNonce: RequestNonce
  ): Boolean = {
    RequestNoncesDAO
      .available(participantId, requestNonce)
      .transact(database)
      .unsafeRunSync()
  }

  "burn" should {
    "burn a nonce" in {
      val participantId = createParticipant(
        ParticipantType.Issuer,
        "iohk",
        DataPreparation.newDID(),
        None,
        None
      )
      val nonce = RequestNonce("test".getBytes.toVector)

      available(participantId, nonce) must be(true)
      val result =
        Try(requestNoncesRepository.burn(participantId, nonce).unsafeRunSync())
      result.toOption.value must be(())

      available(participantId, nonce) must be(false)
    }

    "fail if the nonce is already burnt" in {
      val participantId = createParticipant(
        ParticipantType.Issuer,
        "iohk",
        DataPreparation.newDID(),
        None,
        None
      )
      val nonce = RequestNonce("test".getBytes.toVector)
      requestNoncesRepository.burn(participantId, nonce).unsafeRunSync()
      intercept[RuntimeException] {
        requestNoncesRepository.burn(participantId, nonce).unsafeRunSync()
      }
    }

    "burn the same nonce for several participants" in {
      val participantId = createParticipant(
        ParticipantType.Issuer,
        "iohk",
        DataPreparation.newDID(),
        None,
        None
      )
      val participantId2 = createParticipant(
        ParticipantType.Issuer,
        "iohk-2",
        DataPreparation.newDID(),
        None,
        None
      )
      val nonce = RequestNonce("test".getBytes.toVector)
      requestNoncesRepository.burn(participantId, nonce).unsafeRunSync()

      val result =
        Try(requestNoncesRepository.burn(participantId2, nonce).unsafeRunSync())
      result.toOption.value must be(())

      available(participantId, nonce) must be(false)
      available(participantId2, nonce) must be(false)
    }
  }
}
