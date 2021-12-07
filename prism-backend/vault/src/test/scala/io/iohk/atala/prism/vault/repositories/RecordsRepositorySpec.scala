package io.iohk.atala.prism.vault.repositories

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import io.iohk.atala.prism.AtalaWithPostgresSpec
import io.iohk.atala.prism.vault.model.{CreateRecord, Record}
import io.iohk.atala.prism.utils.IOUtils._
import io.iohk.atala.prism.vault.TestUtils.{randomRecordId, randomRecordType}
import org.scalatest.OptionValues
import tofu.logging.Logs

class RecordsRepositorySpec extends AtalaWithPostgresSpec with OptionValues {

  private lazy val vaultTestLogs: Logs[IO, IO] = Logs.sync[IO, IO]

  lazy val repo: RecordsRepository[IO] = RecordsRepository.unsafe(database, vaultTestLogs)

  def createRecord(payload: Vector[Byte], recordType: Record.Type = randomRecordType()): IO[Record] = {
    val recordId = randomRecordId()
    val createPayload1 = CreateRecord(recordType, recordId, Record.Payload(payload))
    repo.create(createPayload1)
  }

  "create" should {
    "create a new record" in {
      val content1 = "encrypted_data_1".getBytes.toVector
      val content2 = "encrypted_data_2".getBytes.toVector

      val test = for {
        record1 <- createRecord(content1)
        record2 <- createRecord(content2)
      } yield {
        record1.payload.encrypted must be(content1)
        record2.payload.encrypted must be(content2)
      }
      test.unsafeRunSync()
    }
  }

  "getBy" should {
    "return all created payloads" in {
      val content1 = "encrypted_data_1".getBytes.toVector

      val content2 = "encrypted_data_2".getBytes.toVector

      val content3 = "encrypted_data_3".getBytes.toVector
      val recordType = randomRecordType()

      val test = for {
        record1 <- createRecord(content1, recordType)
        _ <- createRecord(content2)
        record3 <- createRecord(content3, recordType)
        mustBe1And3Payload <- repo.getRecordsPaginated(recordType, None, 10)
        mustBe1Payload <- repo.getRecordsPaginated(recordType, None, 1)
        mustBe3Payload <- repo.getRecordsPaginated(recordType, Some(record1.id), 10)
      } yield {
        mustBe1And3Payload mustBe List(record1, record3)
        mustBe1Payload mustBe List(record1)
        mustBe3Payload mustBe List(record3)
      }
      test.unsafeRunSync()
    }
  }
}
