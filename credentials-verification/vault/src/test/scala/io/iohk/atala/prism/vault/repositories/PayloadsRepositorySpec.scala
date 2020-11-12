package io.iohk.atala.prism.vault.repositories

import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

import io.iohk.atala.prism.repositories.PostgresRepositorySpec
import io.iohk.atala.prism.vault.model.{CreatePayload, Payload}
import org.scalatest.OptionValues
import cats.scalatest.EitherMatchers._
import io.iohk.atala.prism.crypto.SHA256Digest
import io.iohk.atala.prism.identity.DID

class PayloadsRepositorySpec extends PostgresRepositorySpec with OptionValues {
  lazy val repository = new PayloadsRepository(database)

  def createPayload(did: DID, content: Vector[Byte]): Payload = {
    val externalId = Payload.ExternalId(UUID.randomUUID())
    val hash = SHA256Digest.compute(content.toArray)
    val createPayload1 = CreatePayload(externalId, hash, did, content)
    repository.create(createPayload1).value.futureValue.toOption.value
  }

  "create" should {
    "create a new payload" in {
      val did1 = DID.buildPrismDID("test1")
      val content1 = "encrypted_data_1".getBytes.toVector
      val payload1 = createPayload(did1, content1)

      val did2 = DID.buildPrismDID("test2")
      val content2 = "encrypted_data_2".getBytes.toVector
      val payload2 = createPayload(did2, content2)

      payload1.did must be(did1)
      payload1.content must be(content1)
      assert(payload1.createdAt.until(Instant.now(), ChronoUnit.MINUTES) <= 2)

      payload2.did must be(did2)
      payload2.content must be(content2)
      assert(payload2.createdAt.until(Instant.now(), ChronoUnit.MINUTES) <= 2)

      assert(payload1.createdAt.isBefore(payload2.createdAt))
    }
  }

  "getBy" should {
    "return all created payloads" in {
      val did1 = DID.buildPrismDID("test1")
      val content1 = "encrypted_data_1".getBytes.toVector
      val payload1 = createPayload(did1, content1)

      val did2 = DID.buildPrismDID("test2")
      val content2 = "encrypted_data_2".getBytes.toVector
      val payload2 = createPayload(did2, content2)

      val content3 = "encrypted_data_3".getBytes.toVector
      val payload3 = createPayload(did2, content3)

      repository.getByPaginated(did1, None, 10).value.futureValue must beRight(List(payload1))
      repository.getByPaginated(did2, None, 10).value.futureValue must beRight(List(payload2, payload3))
      repository.getByPaginated(did2, None, 1).value.futureValue must beRight(List(payload2))
      repository.getByPaginated(did2, Some(payload2.id), 1).value.futureValue must beRight(
        List(payload3)
      )
    }
  }
}
