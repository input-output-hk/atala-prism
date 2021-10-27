package io.iohk.atala.prism.node.repositories

import cats.effect.IO
import io.iohk.atala.prism.AtalaWithPostgresSpec
import io.iohk.atala.prism.node.repositories.daos.KeyValuesDAO.KeyValue
import tofu.logging.Logging

class KeyValuesRepositorySpec extends AtalaWithPostgresSpec {
  private val logs = Logging.Make.plain[IO]
  private lazy val keyValuesRepository =
    KeyValuesRepository.unsafe(database, logs)

  private val KEY = "test-key"
  private val VALUE = "Test value"

  "KeyValuesRepository" should {
    "insert a KeyValue when it does not exist" in {
      val expectedKeyValue = KeyValue(KEY, Some(VALUE))
      keyValuesRepository.upsert(expectedKeyValue).unsafeToFuture().futureValue

      val keyValue = keyValuesRepository.get(KEY).unsafeToFuture().futureValue

      keyValue must be(expectedKeyValue)
    }

    "update a KeyValue when it exists" in {
      keyValuesRepository
        .upsert(KeyValue(KEY, Some("Old value")))
        .unsafeToFuture()
        .futureValue
      val expectedKeyValue = KeyValue(KEY, Some(VALUE))
      keyValuesRepository.upsert(expectedKeyValue).unsafeToFuture().futureValue

      val keyValue = keyValuesRepository.get(KEY).unsafeToFuture().futureValue

      keyValue must be(expectedKeyValue)
    }

    "clear a KeyValue when set to None" in {
      keyValuesRepository
        .upsert(KeyValue(KEY, Some("Old value")))
        .unsafeToFuture()
        .futureValue
      val expectedKeyValue = KeyValue(KEY, None)
      keyValuesRepository.upsert(expectedKeyValue).unsafeToFuture().futureValue

      val keyValue = keyValuesRepository.get(KEY).unsafeToFuture().futureValue

      keyValue must be(expectedKeyValue)
    }

    "return no value when it does not exist" in {
      val keyValue = keyValuesRepository.get(KEY).unsafeToFuture().futureValue

      keyValue must be(KeyValue(KEY, None))
    }
  }
}
