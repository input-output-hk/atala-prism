package io.iohk.atala.prism.management.console.repositories

import doobie.implicits._
import io.iohk.atala.prism.identity.DID
import io.iohk.atala.prism.management.console.errors.UnknownValueError
import io.iohk.atala.prism.management.console.models.{ParticipantId, ParticipantInfo}
import io.iohk.atala.prism.management.console.repositories.daos.ParticipantsDAO
import io.iohk.atala.prism.repositories.PostgresRepositorySpec
import org.scalatest.EitherValues._
import org.scalatest.OptionValues._

import scala.concurrent.duration.DurationLong

class ParticipantsRepositorySpec extends PostgresRepositorySpec {

  implicit val pc: PatienceConfig = PatienceConfig(20.seconds, 5.millis)
  lazy val participantsRepository = new ParticipantsRepository(database)

  "getParticipant by did" should {
    "get a participant" in {
      val id = ParticipantId.random()
      val did = DID("did:prism:test")
      val info = ParticipantInfo(id, "issuer", did, None)
      ParticipantsDAO
        .insert(info)
        .transact(database)
        .unsafeToFuture()
        .futureValue

      val result = participantsRepository.findBy(did).value.futureValue
      result.toOption.value must be(info)
    }

    "return no participant on unknown did" in {
      val did = DID("did:prism:test")
      ParticipantsDAO
        .insert(
          ParticipantInfo(
            ParticipantId.random(),
            "issuer",
            DID(did.value + "x"),
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
