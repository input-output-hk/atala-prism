package io.iohk.atala.prism.management.console.repositories

import doobie.implicits._
import io.iohk.atala.prism.AtalaWithPostgresSpec
import io.iohk.atala.prism.management.console.DataPreparation
import io.iohk.atala.prism.management.console.errors.UnknownValueError
import io.iohk.atala.prism.management.console.models.{ParticipantId, ParticipantInfo}
import io.iohk.atala.prism.management.console.repositories.daos.ParticipantsDAO
import org.scalatest.EitherValues._
import org.scalatest.OptionValues._

class ParticipantsRepositorySpec extends AtalaWithPostgresSpec {
  lazy val participantsRepository = new ParticipantsRepository(database)

  "getParticipant by did" should {
    "get a participant" in {
      val id = ParticipantId.random()
      val did = DataPreparation.newDID()
      val info = ParticipantInfo(id, "test-name", did, None)
      ParticipantsDAO
        .insert(info)
        .transact(database)
        .unsafeToFuture()
        .futureValue

      val result = participantsRepository.findBy(did).value.futureValue
      result.toOption.value must be(info)
    }

    "return no participant on unknown did" in {
      ParticipantsDAO
        .insert(
          ParticipantInfo(
            ParticipantId.random(),
            "test-name",
            DataPreparation.newDID(),
            None
          )
        )
        .transact(database)
        .unsafeToFuture()
        .futureValue

      val did = DataPreparation.newDID()
      val result = participantsRepository.findBy(did).value.futureValue
      result.left.value must be(UnknownValueError("did", did.value))
    }
  }
}
