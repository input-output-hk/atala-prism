package io.iohk.atala.prism.management.console.repositories

import cats.effect.IO

import java.time.Instant
import doobie.implicits._
import doobie.implicits.legacy.instant._
import io.iohk.atala.prism.auth.model.RequestNonce
import io.iohk.atala.prism.management.console.models.ParticipantId
import io.iohk.atala.prism.management.console.repositories.daos._
import io.iohk.atala.prism.AtalaWithPostgresSpec
import io.iohk.atala.prism.repositories.ops.SqlTestOps.Implicits
import io.iohk.atala.prism.utils.IOUtils._
import org.scalatest.OptionValues._
import tofu.logging.Logs

import scala.util.Try

class RequestNoncesRepositorySpec extends AtalaWithPostgresSpec {
  private def createParticipant(
      name: String,
      did: String
  ): ParticipantId = {
    val createdAt = Instant.now()
    sql"""INSERT INTO participants(participant_id, did, name, created_at) VALUES
          (${ParticipantId.random()}, $did, $name, $createdAt)
          RETURNING participant_id"""
      .runUnique[ParticipantId]()
  }

  val logs: Logs[IO, IO] = Logs.sync[IO, IO]

  lazy val requestNoncesRepository =
    RequestNoncesRepository.unsafe(database, logs)

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
      val participantId = createParticipant("iohk", "did:prism:iohk")
      val nonce = RequestNonce("test".getBytes.toVector)

      available(participantId, nonce) must be(true)
      val result =
        Try(requestNoncesRepository.burn(participantId, nonce).unsafeRunSync())
      result.toOption.value must be(())

      available(participantId, nonce) must be(false)
    }

    "fail if the nonce is already burnt" in {
      val participantId = createParticipant("iohk", "did:prism:iohk")
      val nonce = RequestNonce("test".getBytes.toVector)
      requestNoncesRepository.burn(participantId, nonce).unsafeRunSync()
      intercept[RuntimeException] {
        requestNoncesRepository.burn(participantId, nonce).unsafeRunSync()
      }
    }

    "burn the same nonce for several participants" in {
      val participantId = createParticipant("iohk", "did:prism:iohk")
      val participantId2 = createParticipant("iohk-2", "did:prism:iohk-2")
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
