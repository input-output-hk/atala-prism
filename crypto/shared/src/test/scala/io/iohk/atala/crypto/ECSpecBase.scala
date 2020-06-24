package io.iohk.atala.crypto

import org.scalatest.matchers.must.Matchers._
import org.scalatest.wordspec.AnyWordSpec

/**
  * Base class to be extended by tests for {@link ECTrait} implementations.
  * @param ec the implementation under test
  */
abstract class ECSpecBase(val ec: ECTrait) extends AnyWordSpec {
  "EC" should {
    "generate a key pair" in {
      val keyPair = ec.generateKeyPair()

      keyPair.getPrivateKey.getHexEncoded must not be empty
      keyPair.getPublicKey.getHexEncoded must not be empty
      // TODO: Fix inconsistencies between JS and JVM and perform better testing
    }
  }
}
