package io.iohk.atala.prism.credentials

import java.time.Instant

import io.circe.Json
import io.iohk.atala.prism.credentials.VerificationError.{InvalidSignature, KeyWasNotValid, KeyWasRevoked, Revoked}
import io.iohk.atala.prism.crypto.ECTrait
import org.scalatest.OptionValues.convertOptionToValuable
import org.scalatest.matchers.must.Matchers._
import org.scalatest.wordspec.AnyWordSpec

abstract class CredentialVerificationSpecBase extends AnyWordSpec with ValidatedValues {
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

      CredentialVerification.verifyCredential(keyData, credentialData, signedCredential).isValid mustBe true
    }

    "return false when credential is revoked" in {
      val keys = ec.generateKeyPair()
      val keyData = KeyData(publicKey = keys.publicKey, addedOn = before, revokedOn = None)
      val credentialData = CredentialData(issuedOn = now, revokedOn = Some(after))
      val signedCredential = CredentialsCryptoSDKImpl.signCredential(unsignedCredential, keys.privateKey)

      val e = CredentialVerification.verifyCredential(keyData, credentialData, signedCredential).invalid
      e.size must be(1)
      e.toList must contain(Revoked(credentialData.revokedOn.value))
    }

    "return false when credential added before key" in {
      val keys = ec.generateKeyPair()
      val keyData = KeyData(publicKey = keys.publicKey, addedOn = now, revokedOn = None)
      val credentialData = CredentialData(issuedOn = before, revokedOn = None)
      val signedCredential = CredentialsCryptoSDKImpl.signCredential(unsignedCredential, keys.privateKey)

      val e = CredentialVerification.verifyCredential(keyData, credentialData, signedCredential).invalid
      e.size must be(1)
      e.toList must contain(KeyWasNotValid(keyData.addedOn, credentialData.issuedOn))
    }

    "return false when key is revoked before credential is added" in {
      val keys = ec.generateKeyPair()
      val keyData = KeyData(publicKey = keys.publicKey, addedOn = before, revokedOn = Some(now))
      val credentialData = CredentialData(issuedOn = after, revokedOn = None)
      val signedCredential = CredentialsCryptoSDKImpl.signCredential(unsignedCredential, keys.privateKey)

      val e = CredentialVerification.verifyCredential(keyData, credentialData, signedCredential).invalid
      e.size must be(1)
      e.toList must contain(KeyWasRevoked(credentialData.issuedOn, keyData.revokedOn.value))
    }

    "return false when signature is invalid" in {
      val keys = ec.generateKeyPair()
      val keyData = KeyData(publicKey = keys.publicKey, addedOn = before, revokedOn = None)
      val credentialData = CredentialData(issuedOn = now, revokedOn = None)
      // Sign with different key
      val signedCredential =
        CredentialsCryptoSDKImpl.signCredential(unsignedCredential, ec.generateKeyPair().privateKey)

      val e = CredentialVerification.verifyCredential(keyData, credentialData, signedCredential).invalid
      e.size must be(1)
      e.toList must contain(InvalidSignature)
    }

    "return false when key was added after the credential was issued AND the credential was revoked" in {
      val keys = ec.generateKeyPair()
      val keyData = KeyData(publicKey = keys.publicKey, addedOn = now, revokedOn = Some(after))
      // note that the key was revoked AFTER the credential is issued, this leads to not return a KeyWasRevoked
      val credentialData = CredentialData(issuedOn = before, revokedOn = Some(after))
      val signedCredential = CredentialsCryptoSDKImpl.signCredential(unsignedCredential, keys.privateKey)

      val e = CredentialVerification.verifyCredential(keyData, credentialData, signedCredential).invalid
      e.size must be(2)
      e.toList must contain(KeyWasNotValid(keyData.addedOn, credentialData.issuedOn))
      e.toList must contain(Revoked(credentialData.revokedOn.value))
    }

    "return false when key was revoked before the credential is issued AND the credential was revoked" in {
      val keys = ec.generateKeyPair()
      val keyData = KeyData(publicKey = keys.publicKey, addedOn = before, revokedOn = Some(before))
      // note that the key was revoked BEFORE the credential is issued, this leads to return a KeyWasRevoked
      val credentialData = CredentialData(issuedOn = now, revokedOn = Some(after))
      val signedCredential = CredentialsCryptoSDKImpl.signCredential(unsignedCredential, keys.privateKey)

      val e = CredentialVerification.verifyCredential(keyData, credentialData, signedCredential).invalid
      e.size must be(2)
      e.toList must contain(Revoked(credentialData.revokedOn.value))
      e.toList must contain(KeyWasRevoked(credentialData.issuedOn, keyData.revokedOn.value))
    }

    "return false when key was revoked before the credential is issued AND the credential was revoked AND the signature was invalid" in {
      val keys = ec.generateKeyPair()
      val keyData = KeyData(publicKey = keys.publicKey, addedOn = before, revokedOn = Some(before))
      // note that the key was revoked BEFORE the credential is issued, this leads to return a KeyWasRevoked
      val credentialData = CredentialData(issuedOn = now, revokedOn = Some(after))
      // Sign with different key
      val signedCredential =
        CredentialsCryptoSDKImpl.signCredential(unsignedCredential, ec.generateKeyPair().privateKey)

      val e = CredentialVerification.verifyCredential(keyData, credentialData, signedCredential).invalid
      e.size must be(3)
      e.toList must contain(Revoked(credentialData.revokedOn.value))
      e.toList must contain(KeyWasRevoked(credentialData.issuedOn, keyData.revokedOn.value))
      e.toList must contain(InvalidSignature)
    }
  }
}
