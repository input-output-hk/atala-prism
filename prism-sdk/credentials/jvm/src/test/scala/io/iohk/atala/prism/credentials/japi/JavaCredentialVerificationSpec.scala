package io.iohk.atala.prism.credentials.japi

import java.time.Instant
import java.util.Optional

import io.iohk.atala.prism.credentials.japi.verification.error.VerificationException.ErrorCode
import io.iohk.atala.prism.credentials.json.JsonBasedCredential
import io.iohk.atala.prism.credentials.content.CredentialContent
import io.iohk.atala.prism.credentials.content.syntax._
import io.iohk.atala.prism.crypto.japi.{CryptoProvider, EC, ECPrivateKey, ECPrivateKeyFacade}
import io.iohk.atala.prism.crypto.{AndroidEC, ECTrait, ECPrivateKey => JvmECPrivateKey}
import io.iohk.atala.prism.identity.DID
import org.scalatest.matchers.must.Matchers._
import org.scalatest.wordspec.AnyWordSpec

class JavaCredentialVerificationSpec extends AnyWordSpec {
  private val before = new TimestampInfo(Instant.now().minusSeconds(1), 1, 1)
  private val now = new TimestampInfo(Instant.now(), 2, 2)
  private val after = new TimestampInfo(Instant.now().plusSeconds(1), 3, 3)

  "verifyCredential" should {
    // Only test the Android implementation, as we're more interested in the mappings
    implicit val jvmEc: ECTrait = AndroidEC
    val ec = EC.getInstance(CryptoProvider.Android)

    val credentialContent: CredentialContent =
      CredentialContent(
        "credentialType" -> CredentialContent.Values("VerifiableCredential", "RedlandIdCredential"),
        "issuerDid" -> DID.buildPrismDID("123456678abcdefg").value,
        "issuanceKeyId" -> "Issuance-0"
      )

    lazy val jsonBasedCredential =
      JsonBasedCredential
        .fromCredentialContent(credentialContent)

    "return true when valid" in {
      val keys = ec.generateKeyPair()
      val keyData = new KeyData(keys.getPublic, before, Optional.empty())
      val credentialData = new CredentialData(now, Optional.empty())
      val signedCredential = jsonBasedCredential.sign(toECPrivateKey(keys.getPrivate))
      val wrappedSigneCredential = new JsonBasedCredentialFacade(signedCredential, CredentialContentFacade)

      PrismCredentialVerification
        .verify(keyData, credentialData, wrappedSigneCredential, ec)
        .isValid mustBe true
    }

    "return false when credential is revoked" in {
      val keys = ec.generateKeyPair()
      val keyData = new KeyData(keys.getPublic, before, Optional.empty())
      val credentialData = new CredentialData(now, Optional.of(after))
      val signedCredential = jsonBasedCredential.sign(toECPrivateKey(keys.getPrivate))
      val wrappedSigneCredential = new JsonBasedCredentialFacade(signedCredential, CredentialContentFacade)

      val verificationResult = PrismCredentialVerification.verify(
        keyData,
        credentialData,
        wrappedSigneCredential,
        ec
      )

      verificationResult.isValid mustBe false
      verificationResult.getVerificationExceptions.size() mustBe 1
      verificationResult.getVerificationExceptions.get(0).getCode mustBe ErrorCode.Revoked
    }

    "return false when credential added before key" in {
      val keys = ec.generateKeyPair()
      val keyData = new KeyData(keys.getPublic, now, Optional.empty())
      val credentialData = new CredentialData(before, Optional.empty())
      val signedCredential = jsonBasedCredential.sign(toECPrivateKey(keys.getPrivate))
      val wrappedSigneCredential = new JsonBasedCredentialFacade(signedCredential, CredentialContentFacade)

      val verificationResult = PrismCredentialVerification.verify(
        keyData,
        credentialData,
        wrappedSigneCredential,
        ec
      )

      verificationResult.isValid mustBe false
      verificationResult.getVerificationExceptions.size() mustBe 1
      verificationResult.getVerificationExceptions.get(0).getCode mustBe ErrorCode.KeyWasNotValid
    }

    "return false when key is revoked before credential is added" in {
      val keys = ec.generateKeyPair()
      val keyData = new KeyData(keys.getPublic, before, Optional.of(now))
      val credentialData = new CredentialData(after, Optional.empty())
      val signedCredential = jsonBasedCredential.sign(toECPrivateKey(keys.getPrivate))
      val wrappedSigneCredential = new JsonBasedCredentialFacade(signedCredential, CredentialContentFacade)

      val verificationResult = PrismCredentialVerification.verify(
        keyData,
        credentialData,
        wrappedSigneCredential,
        ec
      )

      verificationResult.isValid mustBe false
      verificationResult.getVerificationExceptions.size() mustBe 1
      verificationResult.getVerificationExceptions.get(0).getCode mustBe ErrorCode.KeyWasRevoked
    }

    "return false when signature is invalid" in {
      val keys = ec.generateKeyPair()
      val keyData = new KeyData(keys.getPublic, before, Optional.empty())
      val credentialData = new CredentialData(now, Optional.empty())
      // Sign with different key
      val signedCredential = jsonBasedCredential.sign(toECPrivateKey(ec.generateKeyPair().getPrivate))
      val wrappedSigneCredential = new JsonBasedCredentialFacade(signedCredential, CredentialContentFacade)

      val verificationResult = PrismCredentialVerification.verify(
        keyData,
        credentialData,
        wrappedSigneCredential,
        ec
      )

      verificationResult.isValid mustBe false
      verificationResult.getVerificationExceptions.size() mustBe 1
      verificationResult.getVerificationExceptions.get(0).getCode mustBe ErrorCode.InvalidSignature
    }

    def toECPrivateKey(privateKey: ECPrivateKey): JvmECPrivateKey = {
      privateKey.asInstanceOf[ECPrivateKeyFacade].privateKey
    }
  }
}
