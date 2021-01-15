package io.iohk.atala.prism.management.console.repositories

import doobie.implicits._
import io.iohk.atala.prism.identity.DID
import io.iohk.atala.prism.management.console.errors.UnknownValueError
import io.iohk.atala.prism.management.console.models.{ParticipantId, ParticipantInfo}
import io.iohk.atala.prism.management.console.repositories.daos.ParticipantsDAO
import io.iohk.atala.prism.AtalaWithPostgresSpec
import org.scalatest.EitherValues._
import org.scalatest.OptionValues._

import scala.concurrent.duration.DurationLong

class ParticipantsRepositorySpec extends AtalaWithPostgresSpec {

  implicit val pc: PatienceConfig = PatienceConfig(20.seconds, 5.millis)
  lazy val participantsRepository = new ParticipantsRepository(database)

  "getParticipant by did" should {
    "get a participant" in {
      val id = ParticipantId.random()
      val did = DID.buildPrismDID("test")
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
      val did = DID.buildPrismDID("test")
      ParticipantsDAO
        .insert(
          ParticipantInfo(
            ParticipantId.random(),
            "test-name",
            DID.buildPrismDID("test-x"),
            None
          )
        )
        .transact(database)
        .unsafeToFuture()
        .futureValue

      val result = participantsRepository.findBy(did).value.futureValue
      result.left.value must be(UnknownValueError("did", did.value))
    }
  }
}
