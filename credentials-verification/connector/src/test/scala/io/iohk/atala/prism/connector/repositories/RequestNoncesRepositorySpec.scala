package io.iohk.atala.prism.connector.repositories

import doobie.implicits._
import io.iohk.atala.prism.connector.model.{ParticipantType, RequestNonce}
import io.iohk.atala.prism.connector.repositories.daos.RequestNoncesDAO
import io.iohk.atala.prism.models.ParticipantId
import org.scalatest.EitherValues._

class RequestNoncesRepositorySpec extends ConnectorRepositorySpecBase {

  lazy val requestNoncesRepository = new RequestNoncesRepository.PostgresImpl(database)

  private def available(participantId: ParticipantId, requestNonce: RequestNonce): Boolean = {
    RequestNoncesDAO.available(participantId, requestNonce).transact(database).unsafeRunSync()
  }

  "burn" should {
    "burn a nonce" in {
      val participantId = createParticipant(ParticipantType.Issuer, "iohk", "did:test:iohk", None, None)
      val nonce = RequestNonce("test".getBytes.toVector)

      available(participantId, nonce) must be(true)
      val result = requestNoncesRepository.burn(participantId, nonce).value.futureValue
      result.right.value must be(())

      available(participantId, nonce) must be(false)
    }

    "fail if the nonce is already burnt" in {
      val participantId = createParticipant(ParticipantType.Issuer, "iohk", "did:test:iohk", None, None)
      val nonce = RequestNonce("test".getBytes.toVector)
      requestNoncesRepository.burn(participantId, nonce).value.futureValue
      intercept[RuntimeException] {
        requestNoncesRepository.burn(participantId, nonce).value.futureValue
      }
    }

    "burn the same nonce for several participants" in {
      val participantId = createParticipant(ParticipantType.Issuer, "iohk", "did:test:iohk", None, None)
      val participantId2 = createParticipant(ParticipantType.Issuer, "iohk-2", "did:test:iohk-2", None, None)
      val nonce = RequestNonce("test".getBytes.toVector)
      requestNoncesRepository.burn(participantId, nonce).value.futureValue

      val result = requestNoncesRepository.burn(participantId2, nonce).value.futureValue
      result.right.value must be(())

      available(participantId, nonce) must be(false)
      available(participantId2, nonce) must be(false)
    }
  }
}
