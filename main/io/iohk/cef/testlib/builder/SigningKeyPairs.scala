package io.iohk.cef.test.builder

import akka.util.ByteString
import io.iohk.crypto._
import io.iohk.codecs.nio.auto._

trait KeyPairs[T] {

  def gen(): T

  // a set of pre defined keys
  val alice = gen()
  val bob = gen()
  val carlos = gen()
  val daniel = gen()
  val elena = gen()
  val francisco = gen()
  val german = gen()
  val hugo = gen()
}

trait SigningKeyPairs extends KeyPairs[SigningKeyPair] {

  // a set of pre defined keys
  override def gen() = generateSigningKeyPair()

  // a signature that no one can validate
  val uselessSignature = sign(ByteString("input"), generateSigningKeyPair().`private`)
}

object SigningKeyPairs extends SigningKeyPairs

trait EncryptionKeyPairs extends KeyPairs[EncryptionKeyPair] {

  // a set of pre defined keys
  override def gen() = generateEncryptionKeyPair()
}

object EncryptionKeyPairs extends EncryptionKeyPairs
