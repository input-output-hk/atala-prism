package io.iohk.cef.builder

import akka.util.ByteString
import io.iohk.cef.crypto._

trait SigningKeyPairs {

  // a set of pre defined keys
  val alice = generateSigningKeyPair()
  val bob = generateSigningKeyPair()
  val carlos = generateSigningKeyPair()
  val daniel = generateSigningKeyPair()
  val elena = generateSigningKeyPair()
  val francisco = generateSigningKeyPair()
  val german = generateSigningKeyPair()
  val hugo = generateSigningKeyPair()

  // a signature that no one can validate
  val uselessSignature = sign(ByteString("input"), generateSigningKeyPair().`private`)
}
