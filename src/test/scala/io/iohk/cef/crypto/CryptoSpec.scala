package io.iohk.cef.crypto

import org.scalatest.FlatSpec
import org.scalatest.Matchers._
import io.iohk.cef.network.encoding.nio._

/**
  * Validated examples on how the crypto module gets used
  */
class CryptoSpec extends FlatSpec {

  case class User(name: String, age: Int)

  val user = User("Foo Bar", 42)
  val user2 = User("Bar Foo", 24)

  "this hashing examples" should "keep being valid" in {
    val userHash = hash(user)
    isValidHash(user, userHash) should be(true)
    isValidHash(user2, userHash) should be(false)
  }

  "this encryption examples" should "keep being valid" in {
    val EncryptionKeyPair(pubEncryptionKey, privEncryptionKey) = generateEncryptionKeyPair
    val encrypted = encrypt(user, pubEncryptionKey)
    val decrypted = decrypt[User](encrypted, privEncryptionKey)
    decrypted should be(Right(user))
  }

  "this signing examples" should "keep being valid" in {
    val SigningKeyPair(pubSigningKey, privSigningKey) = generateSigningKeyPair
    val signature = sign(user, privSigningKey)
    val isValidSign = isValidSignature(user, signature, pubSigningKey)
    isValidSign should be(true)
    isValidSignature(user2, signature, pubSigningKey) should be(false)

  }

}
