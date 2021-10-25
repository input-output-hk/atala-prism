package io.iohk.atala.prism.management.console.repositories

import cats.effect.IO
import com.typesafe.config.ConfigFactory
import doobie.implicits._
import io.iohk.atala.prism.AtalaWithPostgresSpec
import io.iohk.atala.prism.crypto.EC.{INSTANCE => EC}
import io.iohk.atala.prism.identity.{PrismDid => DID}
import io.iohk.atala.prism.management.console.DataPreparation
import io.iohk.atala.prism.management.console.config.DefaultCredentialTypeConfig
import io.iohk.atala.prism.management.console.errors.{InvalidRequest, UnknownValueError}
import io.iohk.atala.prism.management.console.models.{
  ParticipantId,
  ParticipantInfo,
  ParticipantLogo,
  UpdateParticipantProfile
}
import io.iohk.atala.prism.management.console.repositories.ParticipantsRepository.CreateParticipantRequest
import io.iohk.atala.prism.management.console.repositories.daos.ParticipantsDAO
import io.iohk.atala.prism.utils.IOUtils._
import org.scalatest.EitherValues._
import org.scalatest.OptionValues._

import tofu.logging.Logs

//sbt "project management-console" "testOnly *ParticipantsRepositorySpec"
class ParticipantsRepositorySpec extends AtalaWithPostgresSpec {

  val logs: Logs[IO, IO] = Logs.sync[IO, IO]

  lazy val participantsRepository =
    ParticipantsRepository.unsafe(database, logs)

  "getParticipant by did" should {
    "get a participant" in {
      val id = ParticipantId.random()
      val did = DataPreparation.newDID()
      val info = ParticipantInfo(id, "test-name", did, None)
      ParticipantsDAO
        .insert(info)
        .transact(database)
        .unsafeRunSync()

      val result = participantsRepository.findBy(did).unsafeRunSync()
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
        .unsafeRunSync()

      val did = DataPreparation.newDID()
      val result = participantsRepository.findBy(did).unsafeRunSync()
      result.left.value must be(UnknownValueError("did", did.getValue))
    }
  }

  "create" should {
    "create new participant with default user defined credential types" in {
      val request = CreateParticipantRequest(
        id = ParticipantId.random(),
        name = "participant name",
        did = DID.buildCanonicalFromMasterPublicKey(
          EC.generateKeyPair().getPublicKey
        ),
        logo = ParticipantLogo(Vector.empty)
      )

      participantsRepository
        .create(request)
        .unsafeRunSync() mustBe a[Right[_, _]]

      val credentialTypesRepository =
        CredentialTypeRepository.unsafe(database, logs)

      val defaultCredentialTypes =
        credentialTypesRepository.findByInstitution(request.id).unsafeRunSync()
      defaultCredentialTypes
        .map(_.name)
        .toSet mustBe DefaultCredentialTypeConfig(
        ConfigFactory.load()
      ).defaultCredentialTypes
        .map(_.name)
        .toSet
    }

    "return error while trying to create participant with the same did twice" in {
      val did =
        DID.buildCanonicalFromMasterPublicKey(EC.generateKeyPair().getPublicKey)
      val request1 = CreateParticipantRequest(
        id = ParticipantId.random(),
        name = "participant name",
        did = did,
        logo = ParticipantLogo(Vector.empty)
      )
      val request2 = CreateParticipantRequest(
        id = ParticipantId.random(),
        name = "participant name",
        did = did,
        logo = ParticipantLogo(Vector.empty)
      )

      participantsRepository
        .create(request1)
        .unsafeRunSync()
        .isRight mustBe true
      participantsRepository.create(request2).unsafeRunSync() mustBe Left(
        InvalidRequest("DID already exists")
      )
    }
  }

  "update participant name and logo by ParticipantId" in {

    val id = ParticipantId.random()
    val logo = ParticipantLogo(bytes = "SomeBytes".getBytes.toVector)
    val did = DataPreparation.newDID()
    val info = ParticipantInfo(id, "issuer", did, None)
    ParticipantsDAO
      .insert(info)
      .transact(database)
      .unsafeRunSync()
    val profile = UpdateParticipantProfile("Updated Issuer", Some(logo))

    ParticipantsDAO
      .updateParticipantByID(id, profile)
      .transact(database)
      .unsafeRunSync()

    val expectedParticipant =
      info.copy(did = did, name = "Updated Issuer", logo = Some(logo))

    val result = participantsRepository.findBy(did).unsafeRunSync()
    result.toOption.value must be(expectedParticipant)
  }

  "update participant with empty name and logo by ParticipantId" in {

    val id = ParticipantId.random()
    val did = DataPreparation.newDID()

    val logo = ParticipantLogo(bytes = "SomeBytes".getBytes.toVector)
    val info = ParticipantInfo(id, "issuer", did, Some(logo))
    ParticipantsDAO
      .insert(info)
      .transact(database)
      .unsafeRunSync()
    val profile = UpdateParticipantProfile("", None)

    ParticipantsDAO
      .updateParticipantByID(id, profile)
      .transact(database)
      .unsafeRunSync()

    val expectedParticipant = info.copy(did = did, name = "", logo = None)

    val result = participantsRepository.findBy(did).unsafeRunSync()
    result.toOption.value must be(expectedParticipant)
  }

  "update participant with empty logo by ParticipantId" in {

    val id = ParticipantId.random()
    val did = DataPreparation.newDID()

    val logo = ParticipantLogo(bytes = "SomeBytes".getBytes.toVector)
    val info = ParticipantInfo(id, "issuer", did, Some(logo))
    ParticipantsDAO
      .insert(info)
      .transact(database)
      .unsafeRunSync()
    val profile = UpdateParticipantProfile("Updated Issuer", None)

    ParticipantsDAO
      .updateParticipantByID(id, profile)
      .transact(database)
      .unsafeRunSync()

    val expectedParticipant =
      info.copy(did = did, name = "Updated Issuer", logo = None)

    val result = participantsRepository.findBy(did).unsafeRunSync()
    result.toOption.value must be(expectedParticipant)
  }

  "fail update participant name and logo for invalid ParticipantId" in {

    val id = ParticipantId.random()
    val did = DataPreparation.newDID()

    val logo = ParticipantLogo(bytes = "SomeBytes".getBytes.toVector)
    val info = ParticipantInfo(id, "issuer", did, None)
    ParticipantsDAO
      .insert(info)
      .transact(database)
      .unsafeRunSync()
    val profile = UpdateParticipantProfile("Updated Issuer", Some(logo))
    val invalidID = ParticipantId.random()
    assertThrows[Exception] {
      ParticipantsDAO
        .updateParticipantByID(invalidID, profile)
        .transact(database)
        .unsafeRunSync()
    }

    val expectedParticipant = info.copy(did = did)
    val result = participantsRepository.findBy(did).unsafeRunSync()
    result.toOption.value must be(expectedParticipant)
  }

}
