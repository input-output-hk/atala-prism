package io.iohk.cef.crypto

import org.scalatest.FlatSpec
import org.scalatest.Matchers._
import io.iohk.cef.network.encoding.nio._

class CryptoSpec extends FlatSpec {

  case class User(name: String, age: Int)

  val user = User("Foo Bar", 42)
  val user2 = User("Bar Foo", 24)

  "hashing" should "work" in {
    val userHash = hashEntity(user)
    isValidHash(user, userHash) should be(true)
    isValidHash(user2, userHash) should be(false)
  }

  ignore should "also work" in {

    // Encryption
    val EncryptionKeyPair(pubEncryptionKey, privEncryptionKey) = generateEncryptionKeyPair
    val encrypted = encryptEntity(user, pubEncryptionKey)
    val decrypted = decryptEntity[User](encrypted, privEncryptionKey)
    decrypted should be(Right(user))

    // Signature
    val SigningKeyPair(pubSigningKey, privSigningKey) = generateSigningKeyPair
    val signature = signEntity(user, privSigningKey)
    val isValidSign = isValidSignature(user, signature, pubSigningKey)
    isValidSign should be(true)
    isValidSignature(user2, signature, pubSigningKey) should be(false)

  }

}
