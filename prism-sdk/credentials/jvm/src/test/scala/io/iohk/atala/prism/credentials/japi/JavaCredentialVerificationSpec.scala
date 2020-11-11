package io.iohk.atala.prism.credentials.japi

import java.time.Instant
import java.util.Optional

import io.circe.Json
import io.iohk.atala.prism.credentials.japi.CredentialVerification.Provider
import io.iohk.atala.prism.credentials.japi.verification.error.VerificationError.ErrorCode
import io.iohk.atala.prism.credentials.{
  CredentialsCryptoSDKImpl,
  JsonBasedUnsignedCredential,
  SignedCredential => JvmSignedCredential
}
import io.iohk.atala.prism.crypto.japi.{CryptoProvider, EC, ECPrivateKey, ECPrivateKeyFacade}
import io.iohk.atala.prism.crypto.{AndroidEC, ECTrait, ECPrivateKey => JvmECPrivateKey}
import io.iohk.atala.prism.identity.DID
import org.scalatest.matchers.must.Matchers._
import org.scalatest.wordspec.AnyWordSpec

class JavaCredentialVerificationSpec extends AnyWordSpec {
  "getInstance" should {
    "return an implementation for every provider" in {
      for (provider <- CredentialVerification.Provider.values()) {
        val impl = CredentialVerification.getInstance(provider)
        impl must not be null
      }
    }
  }

  private val before = new TimestampInfo(Instant.now().minusSeconds(1), 1, 1)
  private val now = new TimestampInfo(Instant.now(), 2, 2)
  private val after = new TimestampInfo(Instant.now().plusSeconds(1), 3, 3)

  "verifyCredential" should {
    // Only test the Android implementation, as we're more interested in the mappings
    implicit val jvmEc: ECTrait = AndroidEC
    val ec = EC.getInstance(CryptoProvider.Android)
    val credentialVerification = CredentialVerification.getInstance(Provider.ANDROID)

    val unsignedCredential = JsonBasedUnsignedCredential.jsonBasedUnsignedCredential.buildFrom(
      issuerDID = DID("did:prism:123456678abcdefg"),
      issuanceKeyId = "Issuance-0",
      claims = Json.obj()
    )

    "return true when valid" in {
      val keys = ec.generateKeyPair()
      val keyData = new KeyData(keys.getPublic, before, Optional.empty())
      val credentialData = new CredentialData(now, Optional.empty())
      val signedCredential =
        CredentialsCryptoSDKImpl.signCredential(unsignedCredential, toECPrivateKey(keys.getPrivate))

      credentialVerification
        .verifyCredential(keyData, credentialData, toSignedCredential(signedCredential))
        .isValid mustBe true
    }

    "return false when credential is revoked" in {
      val keys = ec.generateKeyPair()
      val keyData = new KeyData(keys.getPublic, before, Optional.empty())
      val credentialData = new CredentialData(now, Optional.of(after))
      val signedCredential =
        CredentialsCryptoSDKImpl.signCredential(unsignedCredential, toECPrivateKey(keys.getPrivate))

      val verificationResult = credentialVerification.verifyCredential(
        keyData,
        credentialData,
        toSignedCredential(signedCredential)
      )

      verificationResult.isValid mustBe false
      verificationResult.getErrors.size() mustBe 1
      verificationResult.getErrors.get(0).getCode mustBe ErrorCode.Revoked
    }

    "return false when credential added before key" in {
      val keys = ec.generateKeyPair()
      val keyData = new KeyData(keys.getPublic, now, Optional.empty())
      val credentialData = new CredentialData(before, Optional.empty())
      val signedCredential =
        CredentialsCryptoSDKImpl.signCredential(unsignedCredential, toECPrivateKey(keys.getPrivate))

      val verificationResult = credentialVerification.verifyCredential(
        keyData,
        credentialData,
        toSignedCredential(signedCredential)
      )

      verificationResult.isValid mustBe false
      verificationResult.getErrors.size() mustBe 1
      verificationResult.getErrors.get(0).getCode mustBe ErrorCode.KeyWasNotValid
    }

    "return false when key is revoked before credential is added" in {
      val keys = ec.generateKeyPair()
      val keyData = new KeyData(keys.getPublic, before, Optional.of(now))
      val credentialData = new CredentialData(after, Optional.empty())
      val signedCredential =
        CredentialsCryptoSDKImpl.signCredential(unsignedCredential, toECPrivateKey(keys.getPrivate))

      val verificationResult = credentialVerification.verifyCredential(
        keyData,
        credentialData,
        toSignedCredential(signedCredential)
      )

      verificationResult.isValid mustBe false
      verificationResult.getErrors.size() mustBe 1
      verificationResult.getErrors.get(0).getCode mustBe ErrorCode.KeyWasRevoked
    }

    "return false when signature is invalid" in {
      val keys = ec.generateKeyPair()
      val keyData = new KeyData(keys.getPublic, before, Optional.empty())
      val credentialData = new CredentialData(now, Optional.empty())
      // Sign with different key
      val signedCredential =
        CredentialsCryptoSDKImpl.signCredential(unsignedCredential, toECPrivateKey(ec.generateKeyPair().getPrivate))

      val verificationResult = credentialVerification.verifyCredential(
        keyData,
        credentialData,
        toSignedCredential(signedCredential)
      )

      verificationResult.isValid mustBe false
      verificationResult.getErrors.size() mustBe 1
      verificationResult.getErrors.get(0).getCode mustBe ErrorCode.InvalidSignature
    }

    def toECPrivateKey(privateKey: ECPrivateKey): JvmECPrivateKey = {
      privateKey.asInstanceOf[ECPrivateKeyFacade].privateKey
    }

    def toSignedCredential(signedCredential: JvmSignedCredential): SignedCredential = {
      new SignedCredential(
        new Base64URLCredential(signedCredential.credential.value),
        new Base64URLSignature(signedCredential.signature.value)
      )
    }
  }
}
