package io.iohk.atala.prism.connector.repositories

import doobie.implicits._
import io.iohk.atala.prism.auth.model.RequestNonce
import io.iohk.atala.prism.connector.model.ParticipantType
import io.iohk.atala.prism.connector.repositories.daos.RequestNoncesDAO
import io.iohk.atala.prism.console.DataPreparation
import io.iohk.atala.prism.models.ParticipantId
import org.scalatest.OptionValues._

class RequestNoncesRepositorySpec extends ConnectorRepositorySpecBase {

  lazy val requestNoncesRepository = new RequestNoncesRepository.PostgresImpl(database)

  private def available(participantId: ParticipantId, requestNonce: RequestNonce): Boolean = {
    RequestNoncesDAO.available(participantId, requestNonce).transact(database).unsafeRunSync()
  }

  "burn" should {
    "burn a nonce" in {
      val participantId = createParticipant(ParticipantType.Issuer, "iohk", DataPreparation.newDID(), None, None)
      val nonce = RequestNonce("test".getBytes.toVector)

      available(participantId, nonce) must be(true)
      val result = requestNoncesRepository.burn(participantId, nonce).value.futureValue
      result.toOption.value must be(())

      available(participantId, nonce) must be(false)
    }

    "fail if the nonce is already burnt" in {
      val participantId = createParticipant(ParticipantType.Issuer, "iohk", DataPreparation.newDID(), None, None)
      val nonce = RequestNonce("test".getBytes.toVector)
      requestNoncesRepository.burn(participantId, nonce).value.futureValue
      intercept[RuntimeException] {
        requestNoncesRepository.burn(participantId, nonce).value.futureValue
      }
    }

    "burn the same nonce for several participants" in {
      val participantId = createParticipant(ParticipantType.Issuer, "iohk", DataPreparation.newDID(), None, None)
      val participantId2 = createParticipant(ParticipantType.Issuer, "iohk-2", DataPreparation.newDID(), None, None)
      val nonce = RequestNonce("test".getBytes.toVector)
      requestNoncesRepository.burn(participantId, nonce).value.futureValue

      val result = requestNoncesRepository.burn(participantId2, nonce).value.futureValue
      result.toOption.value must be(())

      available(participantId, nonce) must be(false)
      available(participantId2, nonce) must be(false)
    }
  }
}
