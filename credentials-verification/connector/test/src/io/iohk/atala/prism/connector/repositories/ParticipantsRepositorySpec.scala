package io.iohk.atala.prism.connector.repositories

import doobie.implicits._
import io.iohk.atala.prism.connector.errors.UnknownValueError
import io.iohk.atala.prism.connector.model._
import io.iohk.atala.prism.connector.repositories.daos._
import io.iohk.atala.prism.models.ParticipantId
import org.scalatest.EitherValues._

import scala.concurrent.duration.DurationLong

class ParticipantsRepositorySpec extends ConnectorRepositorySpecBase {

  implicit val pc: PatienceConfig = PatienceConfig(20.seconds, 5.millis)
  lazy val participantsRepository = new ParticipantsRepository(database)

  "getParticipant by did" should {
    "get a participant" in {
      val id = ParticipantId.random()
      val did = "did:prism:test"
      val info = ParticipantInfo(id, ParticipantType.Issuer, None, "issuer", Some(did), None)
      ParticipantsDAO
        .insert(info)
        .transact(database)
        .unsafeToFuture()
        .futureValue

      val result = participantsRepository.findBy(did).value.futureValue
      result.right.value must be(info)
    }

    "return no participant on unknown did" in {
      val did = "did:prism:test"
      ParticipantsDAO
        .insert(ParticipantInfo(ParticipantId.random(), ParticipantType.Issuer, None, "issuer", Some(did + "x"), None))
        .transact(database)
        .unsafeToFuture()
        .futureValue

      val result = participantsRepository.findBy(did).value.futureValue
      result.left.value must be(UnknownValueError("did", did))
    }
  }
}
