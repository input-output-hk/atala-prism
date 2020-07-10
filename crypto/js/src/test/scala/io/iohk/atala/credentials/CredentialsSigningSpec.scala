package io.iohk.atala.credentials

import io.iohk.atala.crypto.EC
import org.scalatest.matchers.must.Matchers._
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.TryValues._

class CredentialsSigningSpec extends AnyWordSpec {
  // Test bytes generated randomly with `Array.fill(20)((scala.util.Random.nextInt(256) - 128).toByte)`
  private val TEST_CREDENTIAL_BYTES =
    Array[Byte](-107, 101, 68, 118, 27, 74, 29, 50, -32, 72, 47, -127, -49, 3, -8, -55, -63, -66, 46, 125)

  "CredentialsSigning.signCredential and CredentialsSigning.verifyCredentialSignature" should {
    "verify as valid a properly signed credential" in {
      val keyPair = EC.generateKeyPair()

      val credential = CredentialsCryptoSDKImpl.signCredential(TEST_CREDENTIAL_BYTES, keyPair.privateKey)

      CredentialsCryptoSDKImpl.verifyCredentialSignature(credential, keyPair.publicKey) must be(true)
    }

    "verify as valid a properly signed credential after reconstructing from String" in {
      val keyPair = EC.generateKeyPair()

      val credential = CredentialsCryptoSDKImpl.signCredential(TEST_CREDENTIAL_BYTES, keyPair.privateKey)
      val encoded = credential.canonicalForm
      val recovered = SignedCredential.from(encoded).success.value

      CredentialsCryptoSDKImpl.verifyCredentialSignature(recovered, keyPair.publicKey) must be(true)
    }

    "verify as valid a properly signed credential after reconstructing from array pair" in {
      val keyPair = EC.generateKeyPair()

      val credential = CredentialsCryptoSDKImpl.signCredential(TEST_CREDENTIAL_BYTES, keyPair.privateKey)
      val decomposedCredential = SignedCredential.decompose(credential)
      val recovered = SignedCredential.from(decomposedCredential.credential, decomposedCredential.signature)

      CredentialsCryptoSDKImpl.verifyCredentialSignature(recovered, keyPair.publicKey) must be(true)
    }

    "consider as invalid a signed credential with wrong key" in {
      val keyPair = EC.generateKeyPair()
      val anotherKeyPair = EC.generateKeyPair()

      val credential = CredentialsCryptoSDKImpl.signCredential(TEST_CREDENTIAL_BYTES, keyPair.privateKey)

      CredentialsCryptoSDKImpl.verifyCredentialSignature(credential, anotherKeyPair.publicKey) must be(false)
    }
  }

  "SignedCredential.from" should {

    "obtain the original value when reconstructing from String" in {
      val keyPair = EC.generateKeyPair()

      val credential = CredentialsCryptoSDKImpl.signCredential(TEST_CREDENTIAL_BYTES, keyPair.privateKey)
      val encoded = credential.canonicalForm
      val recovered = SignedCredential.from(encoded).success.value

      credential must be(recovered)
    }

    "obtain the original value when reconstructing from array pair" in {
      val keyPair = EC.generateKeyPair()

      val credential = CredentialsCryptoSDKImpl.signCredential(TEST_CREDENTIAL_BYTES, keyPair.privateKey)
      val decomposedCredential = SignedCredential.decompose(credential)
      val recovered = SignedCredential.from(decomposedCredential.credential, decomposedCredential.signature)

      credential must be(recovered)
    }

    "fail when we provide an invalid format" in {
      SignedCredential.from("stringWithoutDotSeparator").isSuccess must be(false)
    }
  }

  "CredentialsSigning.decompose" should {

    "obtain the original credential when we decompose the credential" in {
      val keyPair = EC.generateKeyPair()

      val credential = CredentialsCryptoSDKImpl.signCredential(TEST_CREDENTIAL_BYTES, keyPair.privateKey)
      val decomposedCredential = SignedCredential.decompose(credential)

      TEST_CREDENTIAL_BYTES must contain theSameElementsAs decomposedCredential.credential
    }

  }
}
