package io.iohk.cef.crypto.hashing

import akka.util.ByteString
import org.scalatest.FlatSpec
import org.scalatest.Matchers._
import org.scalatest.prop.PropertyChecks
import io.iohk.cef.builder.SecureRandomBuilder
import io.iohk.cef.test.ExtraScalacheckGenerators

class HashingAlogorithmSpec
    extends FlatSpec
    with PropertyChecks
    with SecureRandomBuilder
    with ExtraScalacheckGenerators {

  val hashingCollection: HashingAlgorithmsCollection =
    HashingAlgorithmsCollection()

  val all = hashingCollection.HashingAlgorithmType.values

  all.foreach { `type` =>
    val description = `type`.algorithmIdentifier
    val algorithm = `type`.algorithm

    // Here the tests are run
    sharedTest(description, algorithm)
  }

  protected def sharedTest(algorithmDescription: String, algorithm: HashAlgorithm): Unit = {

    s"$algorithmDescription.hash" should "generate a hash" in {
      val expectedLength = algorithm.hash(ByteString("abd")).bytes.size
      forAll { (input: ByteString) =>
        val result = algorithm.hash(input)
        result.bytes.size should be(expectedLength)
      }
    }

    it should "return something different to the input" in {
      forAll { input: ByteString =>
        val result = algorithm.hash(input)
        result.bytes shouldNot be(input)
      }
    }

    it should "be referentially transparent, returning the same hash when the input is the same" in {
      forAll { (input: ByteString) =>
        (algorithm hash input) should be(algorithm hash input)
      }
    }

    it should "return a different hash when the input is not the same" in {
      forAll { (a: ByteString, b: ByteString) =>
        whenever(a != b) {
          (algorithm hash a) shouldNot be(algorithm hash b)
        }
      }
    }

  }
}
