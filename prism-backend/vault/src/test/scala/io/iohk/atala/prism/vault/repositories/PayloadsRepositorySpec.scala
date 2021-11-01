package io.iohk.atala.prism.vault.repositories

import cats.effect.IO
import io.iohk.atala.prism.AtalaWithPostgresSpec
import io.iohk.atala.prism.crypto.{EC, Sha256}
import io.iohk.atala.prism.identity.{PrismDid => DID}
import io.iohk.atala.prism.vault.model.{CreatePayload, Payload}
import io.iohk.atala.prism.utils.IOUtils._
import org.scalatest.OptionValues
import tofu.logging.Logs

import java.time.Instant
import java.time.temporal.ChronoUnit

class PayloadsRepositorySpec extends AtalaWithPostgresSpec with OptionValues {

  private lazy val vaultTestLogs: Logs[IO, IO] = Logs.sync[IO, IO]

  lazy val repo: PayloadsRepository[IO] = PayloadsRepository.unsafe(database, vaultTestLogs)

  def createPayload(did: DID, content: Vector[Byte]): IO[Payload] = {
    val externalId = Payload.ExternalId.random()
    val hash = Sha256.compute(content.toArray)
    val createPayload1 = CreatePayload(externalId, hash, did, content)
    repo.create(createPayload1)
  }

  "create" should {
    "create a new payload" in {
      val did1 = newDID()
      val content1 = "encrypted_data_1".getBytes.toVector

      val did2 = newDID()
      val content2 = "encrypted_data_2".getBytes.toVector

      val test = for {
        payload1 <- createPayload(did1, content1)
        payload2 <- createPayload(did2, content2)
      } yield {
        payload1.did must be(did1)
        payload1.content must be(content1)
        assert(payload1.createdAt.until(Instant.now(), ChronoUnit.MINUTES) <= 2)

        payload2.did must be(did2)
        payload2.content must be(content2)
        assert(payload2.createdAt.until(Instant.now(), ChronoUnit.MINUTES) <= 2)

        assert(payload1.createdAt.isBefore(payload2.createdAt))
      }
      test.unsafeRunSync()
    }
  }

  "getBy" should {
    "return all created payloads" in {
      val did1 = newDID()
      val content1 = "encrypted_data_1".getBytes.toVector

      val did2 = newDID()
      val content2 = "encrypted_data_2".getBytes.toVector

      val content3 = "encrypted_data_3".getBytes.toVector

      val test = for {
        payload1 <- createPayload(did1, content1)
        payload2 <- createPayload(did2, content2)
        payload3 <- createPayload(did2, content3)
        mustBe1Payload <- repo.getByPaginated(did1, None, 10)
        mustBe2And3Payload <- repo.getByPaginated(did2, None, 10)
        mustBe2Payload <- repo.getByPaginated(did2, None, 1)
        mustBe3Payload <- repo.getByPaginated(did2, Some(payload2.id), 1)
      } yield {
        mustBe1Payload mustBe List(payload1)
        mustBe2And3Payload mustBe List(payload2, payload3)
        mustBe2Payload mustBe List(payload2)
        mustBe3Payload mustBe List(payload3)
      }
      test.unsafeRunSync()
    }
  }

  private def newDID(): DID = {
    DID
      .buildLongFormFromMasterPublicKey(
        EC.INSTANCE.generateKeyPair().getPublicKey
      )
      .asCanonical()
  }
}
