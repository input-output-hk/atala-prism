package io.iohk.cef.crypto
package test.utils

import io.iohk.cef.crypto.encoding.TypedByteString
import akka.util.ByteString
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen
import org.scalacheck.Arbitrary
import io.iohk.cef.test.ScalacheckExtensions
import io.iohk.cef.codecs.nio.auto._

trait CryptoEntityArbitraries extends ScalacheckExtensions {

  private val N = 8

  private def genN[T](generator: => T)(quantity: Int): Arbitrary[T] =
    Arbitrary(Gen.oneOf((1 to quantity).map(_ => generator).toSeq))

  implicit lazy val signingKeyPairArbitrary: Arbitrary[SigningKeyPair] =
    genN[SigningKeyPair](generateSigningKeyPair)(N)

  implicit lazy val signingPublicKeyArbitrary: Arbitrary[SigningPublicKey] =
    Arbitrary(arbitrary[SigningKeyPair].map(_.public))

  implicit lazy val signingPrivateKeyArbitrary: Arbitrary[SigningPrivateKey] =
    Arbitrary(arbitrary[SigningKeyPair].map(_.`private`))

  implicit lazy val encryptionKeyPairArbitrary: Arbitrary[EncryptionKeyPair] =
    genN[EncryptionKeyPair](generateEncryptionKeyPair)(N)

  implicit lazy val encryptionPublicKeyArbitrary: Arbitrary[EncryptionPublicKey] =
    Arbitrary(arbitrary[EncryptionKeyPair].map(_.public))

  implicit lazy val encryptionPrivateKeyArbitrary: Arbitrary[EncryptionPrivateKey] =
    Arbitrary(arbitrary[EncryptionKeyPair].map(_.`private`))

  implicit lazy val hashArbitrary: Arbitrary[Hash] =
    Arbitrary(arbitrary[ByteString].map(bs => hash(bs)))

  implicit lazy val typedByteStringArbitrary: Arbitrary[TypedByteString] =
    Arbitrary(for {
      t <- arbitrary[String]
      b <- arbitrary[ByteString]
    } yield TypedByteString(t, b))

  implicit lazy val signatureArbitraries: Arbitrary[Signature] =
    Arbitrary {
      for {
        k <- arbitrary[SigningPrivateKey]
        b <- arbitrary[ByteString]
      } yield sign(b, k)
    }

  implicit lazy val encryptedDataArbitraries: Arbitrary[EncryptedData] =
    Arbitrary {
      for {
        k <- arbitrary[EncryptionPublicKey]
        b <- arbitrary[ByteString]
      } yield encrypt(b, k)
    }

}
