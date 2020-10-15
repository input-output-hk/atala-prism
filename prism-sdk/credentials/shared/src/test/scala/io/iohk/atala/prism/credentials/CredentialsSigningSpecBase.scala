package io.iohk.atala.prism.credentials

import io.circe.Json
import io.iohk.atala.prism.crypto.ECTrait
import org.scalatest.TryValues._
import org.scalatest.matchers.must.Matchers._
import org.scalatest.wordspec.AnyWordSpec

abstract class CredentialsSigningSpecBase extends AnyWordSpec {
  implicit def ec: ECTrait

  import JsonBasedUnsignedCredential.jsonBasedUnsignedCredential

  private val TEST_UNSIGNED_CREDENTIAL =
    UnsignedCredentialBuilder[JsonBasedUnsignedCredential].buildFrom("", "", Json.obj())

  "CredentialsSigning.signCredential and CredentialsSigning.verifyCredentialSignature" should {
    "verify as valid a properly signed credential" in {
      val keyPair = ec.generateKeyPair()

      val credential = CredentialsCryptoSDKImpl.signCredential(TEST_UNSIGNED_CREDENTIAL, keyPair.privateKey)

      CredentialsCryptoSDKImpl.verifyCredentialSignature(credential, keyPair.publicKey) must be(true)
    }

    "verify as valid a properly signed credential after reconstructing from String" in {
      val keyPair = ec.generateKeyPair()

      val credential = CredentialsCryptoSDKImpl.signCredential(TEST_UNSIGNED_CREDENTIAL, keyPair.privateKey)
      val encoded = credential.canonicalForm
      val recovered = SignedCredential.from(encoded).success.value

      CredentialsCryptoSDKImpl.verifyCredentialSignature(recovered, keyPair.publicKey) must be(true)
    }

    "verify as valid a properly signed credential after reconstructing from array pair" in {
      val keyPair = ec.generateKeyPair()

      val credential = CredentialsCryptoSDKImpl.signCredential(TEST_UNSIGNED_CREDENTIAL, keyPair.privateKey)
      val decomposedCredential = credential.decompose
      val recovered = SignedCredential.from(decomposedCredential.credential, decomposedCredential.signature)

      CredentialsCryptoSDKImpl.verifyCredentialSignature(recovered, keyPair.publicKey) must be(true)
    }

    "consider as invalid a signed credential with wrong key" in {
      val keyPair = ec.generateKeyPair()
      val anotherKeyPair = ec.generateKeyPair()

      val credential = CredentialsCryptoSDKImpl.signCredential(TEST_UNSIGNED_CREDENTIAL, keyPair.privateKey)

      CredentialsCryptoSDKImpl.verifyCredentialSignature(credential, anotherKeyPair.publicKey) must be(false)
    }
  }

  "SignedCredential.from" should {

    "obtain the original value when reconstructing from String" in {
      val keyPair = ec.generateKeyPair()

      val credential = CredentialsCryptoSDKImpl.signCredential(TEST_UNSIGNED_CREDENTIAL, keyPair.privateKey)
      val encoded = credential.canonicalForm
      val recovered = SignedCredential.from(encoded).success.value

      credential must be(recovered)
    }

    "obtain the original value when reconstructing from array pair" in {
      val keyPair = ec.generateKeyPair()

      val credential = CredentialsCryptoSDKImpl.signCredential(TEST_UNSIGNED_CREDENTIAL, keyPair.privateKey)
      val decomposedCredential = credential.decompose
      val recovered = SignedCredential.from(decomposedCredential.credential, decomposedCredential.signature)

      credential must be(recovered)
    }

    "fail when we provide an invalid format" in {
      SignedCredential.from("stringWithoutDotSeparator").isSuccess must be(false)
    }
  }

  "CredentialsSigning.decompose" should {

    "obtain the original credential when we decompose the credential" in {
      val keyPair = ec.generateKeyPair()

      val credential = CredentialsCryptoSDKImpl.signCredential(TEST_UNSIGNED_CREDENTIAL, keyPair.privateKey)
      val decomposedCredential = credential.decompose

      TEST_UNSIGNED_CREDENTIAL must be(decomposedCredential.credential)
    }
  }

  "CredentialsSigning.unsignedCredentialBytes and CredentialsSigning.signatureBytes be consistent with input" in {
    val keyPair = ec.generateKeyPair()

    val credential = CredentialsCryptoSDKImpl.signCredential(TEST_UNSIGNED_CREDENTIAL, keyPair.privateKey)
    val decomposedCredential = credential.decompose

    credential.signatureBytes must contain theSameElementsAs decomposedCredential.signature
    credential.unsignedCredentialBytes must contain theSameElementsAs TEST_UNSIGNED_CREDENTIAL.bytes
  }
}
