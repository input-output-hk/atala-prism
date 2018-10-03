package io.iohk.cef.crypto

import akka.util.ByteString
import io.iohk.cef.crypto.encoding.TypedByteStringDecodingError
import io.iohk.cef.network.encoding.nio._
import io.iohk.cef.test.ScalacheckExtensions._
import org.scalatest.WordSpec
import org.scalatest.MustMatchers._
import org.scalatest.prop.GeneratorDrivenPropertyChecks._
import org.scalatest.EitherValues._
import org.scalacheck.Arbitrary._

class HashingSpec extends WordSpec {

  case class User(name: String, age: Int)

  private val userGen = for {
    name <- arbitrary[String]
    age <- arbitrary[Int]
  } yield User(name, age)

  private val userPairs = for {
    user1: User <- userGen
    user2: User <- userGen if user1 != user2
  } yield (user1, user2)

  "hash" should {
    "generate a hashedValue" in {
      val expectedLength = hash(ByteString("abd")).toByteString.size
      forAll { (name: String, age: Int) =>
        val entity = User(name, age)
        val result = hash(entity)
        result.toByteString.size must be(expectedLength)
      }
    }
  }

  "isValidHash" should {
    "match the same bytes" in {
      forAll { (name: String, age: Int) =>
        val user = User(name, age)
        val hashedValue = hash(user)
        val result = isValidHash(user, hashedValue)
        result must be(true)
      }
    }

    "not match different bytes" in {
      forAll(userPairs) {
        case (user1, user2) =>
          val user1Hash = hash(user1)

          val result = isValidHash(user2, user1Hash)

          result must be(false)
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
        val expected = HashDecodeError.DataExtractionError(TypedByteStringDecodingError.NioDecoderFailedToDecodeTBS)
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
        val expected = HashDecodeError.UnsupportedAlgorithm("XHA256")
        result.left.value must be(expected)
      }
    }
  }
}
