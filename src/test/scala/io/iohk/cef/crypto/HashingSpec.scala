package io.iohk.cef.crypto

import akka.util.ByteString
import io.iohk.cef.crypto.encoding.TypedByteStringDecodingError
import org.scalatest.Matchers._
import org.scalatest.prop.PropertyChecks
import org.scalatest.{EitherValues, MustMatchers, WordSpec}
import io.iohk.cef.test.ScalacheckExctensions

class HashingSpec extends WordSpec with MustMatchers with PropertyChecks with EitherValues with ScalacheckExctensions {

  case class User(name: String, age: Int)

  "hash" should {
    "generate a hashedValue on ByteString" in {
      val expectedLength = hash(ByteString("abd")).toByteString.size
      forAll { input: ByteString =>
        val result = hash(input)
        result.toByteString.size must be(expectedLength)
      }
    }

    "return something different to the input" in {
      forAll { input: ByteString =>
        val result = hash(input)
        result.toByteString.toArray mustNot be(input)
      }
    }
  }

  "hash" should {
    "generate a hashedValue on Entity" in {
      val expectedLength = hash(ByteString("abd")).toByteString.size
      forAll { (name: String, age: Int) =>
        val entity = User(name, age)
        val result = hash(entity)
        result.toByteString.size must be(expectedLength)
      }
    }
  }

  "isValidHash bytes" should {
    "match the same bytes" in {
      forAll { input: ByteString =>
        val hashedValue = hash(input)
        val result = isValidHash(input, hashedValue)
        result must be(true)
      }
    }

    "not match different bytes" in {
      forAll { input: ByteString =>
        val hashedValue = hash(input)

        forAll { nested: ByteString =>
          // TODO: There should be a way to filter on the forAll
          if (!nested.sameElements(input)) {
            val result = isValidHash(nested, hashedValue)
            result must be(false)
          }
        }
      }
    }
  }

  "isValidHash entity" should {
    "match the same bytes" in {
      forAll { (name: String, age: Int) =>
        val entity = User(name, age)
        val hashedValue = hash(entity)
        val result = isValidHash(entity, hashedValue)
        result must be(true)
      }
    }

    "not match different bytes" in {
      forAll { (name: String, age: Int) =>
        val entity = User(name, age)
        val hashedValue = hash(entity)

        forAll { (innerName: String, innerAge: Int) =>
          // TODO: There should be a way to filter on the forAll
          if (innerName != name || innerAge != age) {
            val innerEntity = User(innerName, innerAge)
            val result = isValidHash(innerEntity, hashedValue)
            result must be(false)
          }
        }

      }

      forAll { input: ByteString =>
        val hashedValue = hash(input)

        forAll { nested: ByteString =>
          // TODO: There should be a way to filter on the forAll
          if (!nested.sameElements(input)) {
            val result = isValidHash(nested, hashedValue)
            result must be(false)
          }
        }
      }
    }
  }

  "Hash.decodeFrom" should {
    "decode valid hashedValue" in {
      forAll { bytes: ByteString =>
        val hashedValue = hash(bytes)
        val result = Hash.decodeFrom(hashedValue.toByteString)
        result.right.value must be(hashedValue)
      }
    }

    "fail to decode invalid hashedValues" in {
      pending

      forAll { bytes: ByteString =>
        val result = Hash.decodeFrom(bytes)
        val expected = DecodeError.DataExtractionError[Hash](TypedByteStringDecodingError.NioDecoderFailedToDecodeTBS)
        result.left.value must be(expected)
      }
    }

    "fail to decode hashedValues with unsupported algorithms" in {
      val algorithm = "SHA256".flatMap(_.toByte :: 0.toByte :: Nil).toArray
      forAll { bytes: Array[Byte] =>
        val hashedValue = hash(ByteString(bytes))

        val index = hashedValue.toByteString.indexOfSlice(algorithm)
        val corruptedHashBytes = hashedValue.toByteString.updated(index, 'X'.toByte)

        val result = Hash.decodeFrom(corruptedHashBytes)
        val expected = DecodeError.UnsupportedAlgorithm[Hash]("XHA256")
        result.left.value must be(expected)
      }
    }
  }
}
