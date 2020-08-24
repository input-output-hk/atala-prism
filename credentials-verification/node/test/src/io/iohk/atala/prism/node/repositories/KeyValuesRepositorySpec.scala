package io.iohk.atala.prism.node.repositories

import io.iohk.cvp.repositories.PostgresRepositorySpec
import io.iohk.atala.prism.node.repositories.daos.KeyValuesDAO.KeyValue
import org.scalatest.EitherValues._

import scala.concurrent.duration.DurationLong

class KeyValuesRepositorySpec extends PostgresRepositorySpec {

  private implicit val pc: PatienceConfig = PatienceConfig(20.seconds, 500.millis)
  private lazy val keyValuesRepository = new KeyValuesRepository(database)

  private val KEY = "test-key"
  private val VALUE = "Test value"

  "KeyValuesRepository" should {
    "insert a KeyValue when it does not exist" in {
      val expectedKeyValue = KeyValue(KEY, Some(VALUE))
      keyValuesRepository.upsert(expectedKeyValue).value.futureValue

      val keyValue = keyValuesRepository.get(KEY).value.futureValue.right.value

      keyValue must be(expectedKeyValue)
    }

    "update a KeyValue when it exists" in {
      keyValuesRepository.upsert(KeyValue(KEY, Some("Old value"))).value.futureValue
      val expectedKeyValue = KeyValue(KEY, Some(VALUE))
      keyValuesRepository.upsert(expectedKeyValue).value.futureValue

      val keyValue = keyValuesRepository.get(KEY).value.futureValue.right.value

      keyValue must be(expectedKeyValue)
    }

    "clear a KeyValue when set to None" in {
      keyValuesRepository.upsert(KeyValue(KEY, Some("Old value"))).value.futureValue
      val expectedKeyValue = KeyValue(KEY, None)
      keyValuesRepository.upsert(expectedKeyValue).value.futureValue

      val keyValue = keyValuesRepository.get(KEY).value.futureValue.right.value

      keyValue must be(expectedKeyValue)
    }

    "return no value when it does not exist" in {
      val keyValue = keyValuesRepository.get(KEY).value.futureValue.right.value

      keyValue must be(KeyValue(KEY, None))
    }
  }
}
