package io.iohk.cef.crypto

import akka.util.ByteString
import io.iohk.cef.crypto.encoding.TypedByteStringDecodingError
import io.iohk.cef.network.encoding.nio._
import org.scalatest.Matchers._
import org.scalatest.prop.PropertyChecks
import org.scalatest.{EitherValues, MustMatchers, WordSpec}

class HashingSpec extends WordSpec with MustMatchers with PropertyChecks with EitherValues {

  case class User(name: String, age: Int)

  "hashBytes" should {
    "generate a hash" in {
      val expectedLength = hashBytes(ByteString("abd")).toByteString.size
      forAll { input: Array[Byte] =>
        val result = hashBytes(ByteString(input))
        result.toByteString.size must be(expectedLength)
      }
    }

    "return something different to the input" in {
      forAll { input: Array[Byte] =>
        val result = hashBytes(ByteString(input))
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
      forAll { input: Array[Byte] =>
        val hash = hashBytes(ByteString(input))
        val result = isValidHash(ByteString(input), hash)
        result must be(true)
      }
    }

    "not match different bytes" in {
      forAll { input: Array[Byte] =>
        val hash = hashBytes(ByteString(input))

        forAll { nested: Array[Byte] =>
          // TODO: There should be a way to filter on the forAll
          if (!nested.sameElements(input)) {
            val result = isValidHash(ByteString(nested), hash)
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

      forAll { input: Array[Byte] =>
        val hash = hashBytes(ByteString(input))

        forAll { nested: Array[Byte] =>
          // TODO: There should be a way to filter on the forAll
          if (!nested.sameElements(input)) {
            val result = isValidHash(ByteString(nested), hash)
            result must be(false)
          }
        }
      }
    }
  }

  "Hash.decodeFrom" should {
    "decode valid hash" in {
      forAll { bytes: Array[Byte] =>
        val hash = hashBytes(ByteString(bytes))
        val result = Hash.decodeFrom(hash.toByteString)
        result.right.value must be(hash)
      }
    }

    "fail to decode invalid hashes" in {
      pending

      forAll { bytes: Array[Byte] =>
        val result = Hash.decodeFrom(ByteString(bytes))
        val expected = HashDecodeError.DataExtractionError(TypedByteStringDecodingError.NioDecoderFailedToDecodeTBS)
        result.left.value must be(expected)
      }
    }

    "fail to decode hashes with unsupported algorithms" in {
      val algorithm = "KECCAK256".flatMap(_.toByte :: 0.toByte :: Nil).toArray
      forAll { bytes: Array[Byte] =>
        val hash = hashBytes(ByteString(bytes))

        val index = hash.toByteString.indexOfSlice(algorithm)
        val corruptedHashBytes = hash.toByteString.toArray
        corruptedHashBytes(index) = 'X'.toByte

        val result = Hash.decodeFrom(ByteString(corruptedHashBytes))
        val expected = HashDecodeError.UnsupportedAlgorithm("XECCAK256")
        result.left.value must be(expected)
      }
    }
  }
}
