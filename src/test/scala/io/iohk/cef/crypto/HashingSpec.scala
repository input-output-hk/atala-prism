package io.iohk.cef.crypto

import akka.util.ByteString
import io.iohk.cef.crypto.encoding.TypedByteStringDecodingError
import io.iohk.cef.network.encoding.nio._
import org.scalatest.Matchers._
import org.scalatest.prop.PropertyChecks
import org.scalatest.{EitherValues, MustMatchers, WordSpec}
import io.iohk.cef.test.ScalacheckExctensions

class HashingSpec extends WordSpec with MustMatchers with PropertyChecks with EitherValues with ScalacheckExctensions {

  case class User(name: String, age: Int)

  "hashBytes" should {
    "generate a hash" in {
      val expectedLength = hashBytes(ByteString("abd")).toByteString.size
      forAll { input: ByteString =>
        val result = hashBytes(input)
        result.toByteString.size must be(expectedLength)
      }
    }

    "return something different to the input" in {
      forAll { input: ByteString =>
        val result = hashBytes(input)
        result.toByteString.toArray mustNot be(input)
      }
    }
  }

  "hashEntity" should {
    "generate a hash" in {
      val expectedLength = hashBytes(ByteString("abd")).toByteString.size
      forAll { (name: String, age: Int) =>
        val entity = User(name, age)
        val result = hashEntity(entity)
        result.toByteString.size must be(expectedLength)
      }
    }
  }

  "isValidHash bytes" should {
    "match the same bytes" in {
      forAll { input: ByteString =>
        val hash = hashBytes(input)
        val result = isValidHash(input, hash)
        result must be(true)
      }
    }

    "not match different bytes" in {
      forAll { input: ByteString =>
        val hash = hashBytes(input)

        forAll { nested: ByteString =>
          // TODO: There should be a way to filter on the forAll
          if (!nested.sameElements(input)) {
            val result = isValidHash(nested, hash)
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
        val hash = hashEntity(entity)
        val result = isValidHash(entity, hash)
        result must be(true)
      }
    }

    "not match different bytes" in {
      forAll { (name: String, age: Int) =>
        val entity = User(name, age)
        val hash = hashEntity(entity)

        forAll { (innerName: String, innerAge: Int) =>
          // TODO: There should be a way to filter on the forAll
          if (innerName != name || innerAge != age) {
            val innerEntity = User(innerName, innerAge)
            val result = isValidHash(innerEntity, hash)
            result must be(false)
          }
        }

      }

      forAll { input: ByteString =>
        val hash = hashBytes(input)

        forAll { nested: ByteString =>
          // TODO: There should be a way to filter on the forAll
          if (!nested.sameElements(input)) {
            val result = isValidHash(nested, hash)
            result must be(false)
          }
        }
      }
    }
  }

  "Hash.decodeFrom" should {
    "decode valid hash" in {
      forAll { bytes: ByteString =>
        val hash = hashBytes(bytes)
        val result = Hash.decodeFrom(hash.toByteString)
        result.right.value must be(hash)
      }
    }

    "fail to decode invalid hashes" in {
      pending

      forAll { bytes: ByteString =>
        val result = Hash.decodeFrom(bytes)
        val expected = HashDecodeError.DataExtractionError(TypedByteStringDecodingError.NioDecoderFailedToDecodeTBS)
        result.left.value must be(expected)
      }
    }

    "fail to decode hashes with unsupported algorithms" in {
      val algorithm = "SHA256".flatMap(_.toByte :: 0.toByte :: Nil).toArray
      forAll { bytes: Array[Byte] =>
        val hash = hashBytes(ByteString(bytes))

        val index = hash.toByteString.indexOfSlice(algorithm)
        val corruptedHashBytes = hash.toByteString.updated(index, 'X'.toByte)

        val result = Hash.decodeFrom(corruptedHashBytes)
        val expected = HashDecodeError.UnsupportedAlgorithm("XHA256")
        result.left.value must be(expected)
      }
    }
  }
}
