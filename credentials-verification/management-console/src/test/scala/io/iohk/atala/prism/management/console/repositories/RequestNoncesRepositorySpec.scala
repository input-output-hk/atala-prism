package io.iohk.atala.prism.management.console.repositories

import java.time.Instant

import doobie.implicits._
import doobie.implicits.legacy.instant._
import io.iohk.atala.prism.auth.model.RequestNonce
import io.iohk.atala.prism.management.console.models.ParticipantId
import io.iohk.atala.prism.management.console.repositories.daos._
import io.iohk.atala.prism.repositories.PostgresRepositorySpec
import io.iohk.atala.prism.repositories.ops.SqlTestOps.Implicits
import org.scalatest.OptionValues._

class RequestNoncesRepositorySpec extends PostgresRepositorySpec {
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

  lazy val requestNoncesRepository = new RequestNoncesRepository.PostgresImpl(database)

  private def available(participantId: ParticipantId, requestNonce: RequestNonce): Boolean = {
    RequestNoncesDAO.available(participantId, requestNonce).transact(database).unsafeRunSync()
  }

  "burn" should {
    "burn a nonce" in {
      val participantId = createParticipant("iohk", "did:test:iohk")
      val nonce = RequestNonce("test".getBytes.toVector)

      available(participantId, nonce) must be(true)
      val result = requestNoncesRepository.burn(participantId, nonce).value.futureValue
      result.toOption.value must be(())

      available(participantId, nonce) must be(false)
    }

    "fail if the nonce is already burnt" in {
      val participantId = createParticipant("iohk", "did:test:iohk")
      val nonce = RequestNonce("test".getBytes.toVector)
      requestNoncesRepository.burn(participantId, nonce).value.futureValue
      intercept[RuntimeException] {
        requestNoncesRepository.burn(participantId, nonce).value.futureValue
      }
    }

    "burn the same nonce for several participants" in {
      val participantId = createParticipant("iohk", "did:test:iohk")
      val participantId2 = createParticipant("iohk-2", "did:test:iohk-2")
      val nonce = RequestNonce("test".getBytes.toVector)
      requestNoncesRepository.burn(participantId, nonce).value.futureValue

      val result = requestNoncesRepository.burn(participantId2, nonce).value.futureValue
      result.toOption.value must be(())

      available(participantId, nonce) must be(false)
      available(participantId2, nonce) must be(false)
    }
  }
}
