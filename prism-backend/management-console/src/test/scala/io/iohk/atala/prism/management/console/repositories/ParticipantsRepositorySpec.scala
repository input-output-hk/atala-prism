package io.iohk.atala.prism.management.console.repositories

import com.typesafe.config.ConfigFactory
import doobie.implicits._
import io.iohk.atala.prism.AtalaWithPostgresSpec
import io.iohk.atala.prism.crypto.EC
import io.iohk.atala.prism.identity.DID
import io.iohk.atala.prism.management.console.DataPreparation
import io.iohk.atala.prism.management.console.config.DefaultCredentialTypeConfig
import io.iohk.atala.prism.management.console.errors.UnknownValueError
import io.iohk.atala.prism.management.console.models.{ParticipantId, ParticipantInfo, ParticipantLogo}
import io.iohk.atala.prism.management.console.repositories.ParticipantsRepository.CreateParticipantRequest
import io.iohk.atala.prism.management.console.repositories.daos.ParticipantsDAO
import org.scalatest.EitherValues._
import org.scalatest.OptionValues._

//sbt "project management-console" "testOnly *ParticipantsRepositorySpec"
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

  "create new participant with default user defined credential types" in {
    val request = CreateParticipantRequest(
      id = ParticipantId.random(),
      name = "participant name",
      did = DID.createUnpublishedDID(EC.generateKeyPair().publicKey).canonical.value,
      logo = ParticipantLogo(Vector.empty)
    )

    participantsRepository.create(request).value.futureValue mustBe a[Right[_, _]]

    val credentialTypesRepository = new CredentialTypeRepository(database)

    val defaultCredentialTypes = credentialTypesRepository.findByInstitution(request.id).value.futureValue.toOption.get
    defaultCredentialTypes.map(_.name).toSet mustBe DefaultCredentialTypeConfig(
      ConfigFactory.load()
    ).defaultCredentialTypes
      .map(_.name)
      .toSet
  }
}
