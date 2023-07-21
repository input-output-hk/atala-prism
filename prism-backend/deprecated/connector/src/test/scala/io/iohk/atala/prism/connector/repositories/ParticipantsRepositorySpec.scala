package io.iohk.atala.prism.connector.repositories

import cats.effect.unsafe.implicits.global
import doobie.implicits._
import io.iohk.atala.prism.connector.errors.{UnknownValueError, co}
import io.iohk.atala.prism.connector.model._
import io.iohk.atala.prism.connector.repositories.daos._
import io.iohk.atala.prism.connector.DataPreparation
import io.iohk.atala.prism.connector.repositories.ParticipantsRepository.FindByError
import io.iohk.atala.prism.crypto.Sha256Digest
import io.iohk.atala.prism.identity.{PrismDid => DID}
import io.iohk.atala.prism.utils.Base64Utils.decodeURL
import io.iohk.atala.prism.logging.TraceId
import io.iohk.atala.prism.logging.TraceId.IOWithTraceIdContext
import io.iohk.atala.prism.models.ParticipantId
import io.iohk.atala.prism.utils.IOUtils._
import org.scalatest.EitherValues._
import org.scalatest.OptionValues._

class ParticipantsRepositorySpec extends ConnectorRepositorySpecBase {
  lazy val participantsRepository: ParticipantsRepository[IOWithTraceIdContext] =
    ParticipantsRepository.unsafe(dbLiftedToTraceIdIO, connectorRepoSpecLogs)
  private val canonicalSuffix =
    "0f753f41e0f3488ba56bd581d153ae9b3c9040cbcc7a63245b4644a265eb3b77"
  private val encodedStateUsed =
    "CmEKXxJdCgdtYXN0ZXIwEAFCUAoJc2VjcDI1NmsxEiAel_7KEiez4s_e0u8DyJwLkUnVmUHBuWU-0h01nerSNRohAJlR51Vbk49vagehAwQkFvW_fvyM1qa4ileIEYkXs4pF"

  private val shortDID =
    DID.buildCanonical(Sha256Digest.fromHex(canonicalSuffix))
  private val longDID = DID.buildLongForm(
    Sha256Digest.fromHex(canonicalSuffix),
    decodeURL(encodedStateUsed)
  )

  "getParticipant by did" should {
    "get a participant" in {
      val id = ParticipantId.random()
      val did = DataPreparation.newDID()
      val info = ParticipantInfo(
        id,
        ParticipantType.Issuer,
        None,
        "issuer",
        Some(did),
        None,
        None
      )
      ParticipantsDAO
        .insert(info)
        .transact(database)
        .unsafeToFuture()
        .futureValue

      val result = participantsRepository
        .findBy(did)
        .run(TraceId.generateYOLO)
        .unsafeRunSync()
      result.toOption.value must be(info)
    }

    "get a participant by unpublished DID" in {
      val id = ParticipantId.random()
      val info = ParticipantInfo(
        id,
        ParticipantType.Issuer,
        None,
        "issuer",
        Some(shortDID),
        None,
        None
      )
      ParticipantsDAO
        .insert(info)
        .transact(database)
        .unsafeToFuture()
        .futureValue

      val result = participantsRepository
        .findBy(longDID)
        .run(TraceId.generateYOLO)
        .unsafeRunSync()
      result.toOption.value must be(info)
    }

    "get a participant by DID when creating it with an unpublished DID" in {
      val id = ParticipantId.random()
      val info = ParticipantInfo(
        id,
        ParticipantType.Issuer,
        None,
        "issuer",
        Some(longDID),
        None,
        None
      )
      ParticipantsDAO
        .insert(info)
        .transact(database)
        .unsafeToFuture()
        .futureValue

      val result = participantsRepository
        .findBy(shortDID)
        .run(TraceId.generateYOLO)
        .unsafeRunSync()
      result.toOption.value must be(info.copy(did = Some(shortDID)))
    }

    "return no participant on unknown did" in {
      val did = DataPreparation.newDID()
      ParticipantsDAO
        .insert(
          ParticipantInfo(
            ParticipantId.random(),
            ParticipantType.Issuer,
            None,
            "issuer",
            Some(DataPreparation.newDID()),
            None,
            None
          )
        )
        .transact(database)
        .unsafeToFuture()
        .futureValue

      val result = participantsRepository
        .findBy(did)
        .run(TraceId.generateYOLO)
        .unsafeRunSync()
      result.left.value must be(
        co[FindByError](UnknownValueError("did", did.getValue))
      )
    }

    "update participant name and logo by ParticipantId" in {

      val id = ParticipantId.random()
      val logo = ParticipantLogo(bytes = "SomeBytes".getBytes.toVector)
      val info = ParticipantInfo(
        id,
        ParticipantType.Issuer,
        None,
        "issuer",
        Some(longDID),
        None,
        None
      )
      ParticipantsDAO
        .insert(info)
        .transact(database)
        .unsafeToFuture()
        .futureValue
      val profile = UpdateParticipantProfile("Updated Issuer", Some(logo))

      ParticipantsDAO
        .updateParticipantByID(id, profile)
        .transact(database)
        .unsafeToFuture()
        .futureValue

      val expectedParticipant = info.copy(
        did = Some(shortDID),
        name = "Updated Issuer",
        logo = Some(logo)
      )

      val result = participantsRepository
        .findBy(longDID)
        .run(TraceId.generateYOLO)
        .unsafeRunSync()
      result.toOption.value must be(expectedParticipant)
    }

    "update participant with empty name and logo by ParticipantId" in {

      val id = ParticipantId.random()
      val logo = ParticipantLogo(bytes = "SomeBytes".getBytes.toVector)
      val info = ParticipantInfo(
        id,
        ParticipantType.Issuer,
        None,
        "issuer",
        Some(longDID),
        Some(logo),
        None
      )
      ParticipantsDAO
        .insert(info)
        .transact(database)
        .unsafeToFuture()
        .futureValue
      val profile = UpdateParticipantProfile("", None)

      ParticipantsDAO
        .updateParticipantByID(id, profile)
        .transact(database)
        .unsafeToFuture()
        .futureValue

      val expectedParticipant =
        info.copy(did = Some(shortDID), name = "", logo = None)

      val result = participantsRepository
        .findBy(longDID)
        .run(TraceId.generateYOLO)
        .unsafeRunSync()
      result.toOption.value must be(expectedParticipant)
    }

    "update participant with empty logo by ParticipantId" in {

      val id = ParticipantId.random()
      val logo = ParticipantLogo(bytes = "SomeBytes".getBytes.toVector)
      val info = ParticipantInfo(
        id,
        ParticipantType.Issuer,
        None,
        "issuer",
        Some(longDID),
        Some(logo),
        None
      )
      ParticipantsDAO
        .insert(info)
        .transact(database)
        .unsafeToFuture()
        .futureValue
      val profile = UpdateParticipantProfile("Updated Issuer", None)

      ParticipantsDAO
        .updateParticipantByID(id, profile)
        .transact(database)
        .unsafeToFuture()
        .futureValue

      val expectedParticipant =
        info.copy(did = Some(shortDID), name = "Updated Issuer", logo = None)

      val result = participantsRepository
        .findBy(longDID)
        .run(TraceId.generateYOLO)
        .unsafeRunSync()
      result.toOption.value must be(expectedParticipant)
    }

    "fail update participant name and logo for invalid ParticipantId" in {

      val id = ParticipantId.random()
      val logo = ParticipantLogo(bytes = "SomeBytes".getBytes.toVector)
      val info = ParticipantInfo(
        id,
        ParticipantType.Issuer,
        None,
        "issuer",
        Some(longDID),
        None,
        None
      )
      ParticipantsDAO
        .insert(info)
        .transact(database)
        .unsafeToFuture()
        .futureValue
      val profile = UpdateParticipantProfile("Updated Issuer", Some(logo))
      val invalidID = ParticipantId.random()
      assertThrows[Exception] {
        ParticipantsDAO
          .updateParticipantByID(invalidID, profile)
          .transact(database)
          .unsafeToFuture()
          .futureValue
      }

      val expectedParticipant = info.copy(did = Some(shortDID))
      val result = participantsRepository
        .findBy(longDID)
        .run(TraceId.generateYOLO)
        .unsafeRunSync()
      result.toOption.value must be(expectedParticipant)
    }
  }
}
