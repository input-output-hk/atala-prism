package io.iohk.cef.crypto

import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen
import org.scalacheck.Arbitrary
import io.iohk.cef.codecs.nio.auto._
import org.scalatest.FlatSpec
import io.iohk.cef.codecs.nio.CodecTestingHelpers
import io.iohk.cef.builder.SigningKeyPairs
import io.iohk.cef.builder.EncryptionKeyPairs

class EntityCodecsSpec extends FlatSpec with CodecTestingHelpers {

  behavior of "CryptoEntities Codecs"

  object SigningKeyPairArbitraries extends SigningKeyPairs {
    implicit lazy val signingKeyPairArbitrary: Arbitrary[SigningKeyPair] =
      Arbitrary(Gen.oneOf(alice, bob, carlos, daniel, elena, francisco, german, hugo))

    implicit lazy val signingPublicKeyArbitrary: Arbitrary[SigningPublicKey] =
      Arbitrary(arbitrary[SigningKeyPair].map(_.public))

    implicit lazy val signingPrivateKeyArbitrary: Arbitrary[SigningPrivateKey] =
      Arbitrary(arbitrary[SigningKeyPair].map(_.`private`))
  }

  object EncryptionKeyPairArbitraries extends EncryptionKeyPairs {
    implicit lazy val encryptionKeyPairArbitrary: Arbitrary[EncryptionKeyPair] =
      Arbitrary(Gen.oneOf(alice, bob, carlos, daniel, elena, francisco, german, hugo))

    implicit lazy val encryptionPublicKeyArbitrary: Arbitrary[EncryptionPublicKey] =
      Arbitrary(arbitrary[EncryptionKeyPair].map(_.public))

    implicit lazy val encryptionPrivateKeyArbitrary: Arbitrary[EncryptionPrivateKey] =
      Arbitrary(arbitrary[EncryptionKeyPair].map(_.`private`))
  }

  import SigningKeyPairArbitraries._
  import EncryptionKeyPairArbitraries._

  it should "work correctly with SigningPublicKey" in { testWhenNotEncodingType[SigningPublicKey] }
  it should "work correctly with SigningPrivateKey" in { testWhenNotEncodingType[SigningPrivateKey] }
  it should "work correctly with EncryptionPublicKey" in { pending; testWhenNotEncodingType[EncryptionPublicKey] }
  it should "work correctly with EncryptionPrivateKey" in { pending; testWhenNotEncodingType[EncryptionPrivateKey] }

}
