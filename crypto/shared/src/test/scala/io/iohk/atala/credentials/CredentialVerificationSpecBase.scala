package io.iohk.atala.credentials

import java.time.Instant

import io.circe.Json
import io.iohk.atala.crypto.ECTrait
import org.scalatest.matchers.must.Matchers._
import org.scalatest.wordspec.AnyWordSpec

abstract class CredentialVerificationSpecBase extends AnyWordSpec {
  implicit def ec: ECTrait

  private val unsignedCredential = JsonBasedUnsignedCredential.jsonBasedUnsignedCredential.buildFrom(
    issuerDID = "did:prism:123456678abcdefg",
    issuanceKeyId = "Issuance-0",
    claims = Json.obj()
  )

  private val before = TimestampInfo(Instant.now().minusSeconds(1), 1, 1)
  private val now = TimestampInfo(Instant.now(), 2, 2)
  private val after = TimestampInfo(Instant.now().plusSeconds(1), 3, 3)

  "verifyCredential" should {
    "return true when valid" in {
      val keys = ec.generateKeyPair()
      val keyData = KeyData(publicKey = keys.publicKey, addedOn = before, revokedOn = None)
      val credentialData = CredentialData(issuedOn = now, revokedOn = None)
      val signedCredential = CredentialsCryptoSDKImpl.signCredential(unsignedCredential, keys.privateKey)

      CredentialVerification.verifyCredential(keyData, credentialData, signedCredential) mustBe true
    }

    "return false when credential is revoked" in {
      val keys = ec.generateKeyPair()
      val keyData = KeyData(publicKey = keys.publicKey, addedOn = before, revokedOn = None)
      val credentialData = CredentialData(issuedOn = now, revokedOn = Some(after))
      val signedCredential = CredentialsCryptoSDKImpl.signCredential(unsignedCredential, keys.privateKey)

      CredentialVerification.verifyCredential(keyData, credentialData, signedCredential) mustBe false
    }

    "return false when credential added before key" in {
      val keys = ec.generateKeyPair()
      val keyData = KeyData(publicKey = keys.publicKey, addedOn = now, revokedOn = None)
      val credentialData = CredentialData(issuedOn = before, revokedOn = None)
      val signedCredential = CredentialsCryptoSDKImpl.signCredential(unsignedCredential, keys.privateKey)

      CredentialVerification.verifyCredential(keyData, credentialData, signedCredential) mustBe false
    }

    "return false when key is revoked before credential is added" in {
      val keys = ec.generateKeyPair()
      val keyData = KeyData(publicKey = keys.publicKey, addedOn = before, revokedOn = Some(now))
      val credentialData = CredentialData(issuedOn = after, revokedOn = None)
      val signedCredential = CredentialsCryptoSDKImpl.signCredential(unsignedCredential, keys.privateKey)

      CredentialVerification.verifyCredential(keyData, credentialData, signedCredential) mustBe false
    }

    "return false when signature is invalid" in {
      val keys = ec.generateKeyPair()
      val keyData = KeyData(publicKey = keys.publicKey, addedOn = before, revokedOn = None)
      val credentialData = CredentialData(issuedOn = now, revokedOn = None)
      // Sign with different key
      val signedCredential =
        CredentialsCryptoSDKImpl.signCredential(unsignedCredential, ec.generateKeyPair().privateKey)

      CredentialVerification.verifyCredential(keyData, credentialData, signedCredential) mustBe false
    }
  }
}
