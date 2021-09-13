package io.iohk.atala.prism.credentials

import java.time.Instant

import io.iohk.atala.prism.credentials.VerificationError.{
  BatchWasRevoked,
  CredentialWasRevoked,
  InvalidSignature,
  KeyWasNotValid,
  KeyWasRevoked
}
import io.iohk.atala.prism.crypto.ECTrait
import io.iohk.atala.prism.crypto.MerkleTree.{MerkleInclusionProof, MerkleRoot}
import org.scalatest.OptionValues.convertOptionToValuable
import org.scalatest.matchers.must.Matchers._
import org.scalatest.wordspec.AnyWordSpec
import io.iohk.atala.prism.credentials.content.CredentialContent
import io.iohk.atala.prism.credentials.content.syntax._
import io.iohk.atala.prism.identity.PrismDid

abstract class CredentialVerificationSpecBase extends AnyWordSpec with ValidatedValues {
  implicit def ec: ECTrait

  private val unsignedCredential = Credential.fromCredentialContent(
    CredentialContent(
      "issuerDid" -> PrismDid.buildCanonical(Sha256.compute(encodeToByteArray("123456678abcdefg").value,
      "issuanceKeyId" -> "Issuance-0"
    )
  )

  private val before = TimestampInfo(Instant.now().minusSeconds(1), 1, 1)
  private val now = TimestampInfo(Instant.now(), 2, 2)
  private val after = TimestampInfo(Instant.now().plusSeconds(1), 3, 3)

  "verifyCredential" should {
    def rootAndProofFor(cred: Credential): (MerkleRoot, MerkleInclusionProof) = {
      CredentialBatches.batch(List(cred)) match {
        case (root, List(proof)) => (root, proof)
      }
    }

    "return true when valid" in {
      val keys = ec.generateKeyPair()
      val keyData = KeyData(publicKey = keys.publicKey, addedOn = before, revokedOn = None)
      val batchData = BatchData(issuedOn = now, revokedOn = None)
      val signedCredential = unsignedCredential.sign(keys.privateKey)
      val (root, proof) = rootAndProofFor(signedCredential)
      val revokedAt: Option[TimestampInfo] = None

      PrismCredentialVerification
        .verify(
          keyData,
          batchData,
          revokedAt,
          root,
          proof,
          signedCredential
        )
        .isValid mustBe true
    }

    "return proper validation error when the credential was revoked" in {
      val keys = ec.generateKeyPair()
      val keyData = KeyData(publicKey = keys.publicKey, addedOn = before, revokedOn = None)
      val batchData = BatchData(issuedOn = now, revokedOn = None)
      val signedCredential = unsignedCredential.sign(keys.privateKey)
      val (root, proof) = rootAndProofFor(signedCredential)
      val revokedAt: Option[TimestampInfo] = Some(now)

      val e = PrismCredentialVerification
        .verify(
          keyData,
          batchData,
          revokedAt,
          root,
          proof,
          signedCredential
        )
        .invalid
      e.size must be(1)
      e.toList must contain(CredentialWasRevoked(revokedAt.value))
    }

    "return proper validation error when credential batch is revoked" in {
      val keys = ec.generateKeyPair()
      val keyData = KeyData(publicKey = keys.publicKey, addedOn = before, revokedOn = None)
      val batchData = BatchData(issuedOn = now, revokedOn = Some(after))
      val signedCredential = unsignedCredential.sign(keys.privateKey)
      val (root, proof) = rootAndProofFor(signedCredential)
      val revokedAt: Option[TimestampInfo] = None

      val e = PrismCredentialVerification
        .verify(
          keyData,
          batchData,
          revokedAt,
          root,
          proof,
          signedCredential
        )
        .invalid
      e.size must be(1)
      e.toList must contain(BatchWasRevoked(batchData.revokedOn.value))
    }

    "return proper validation error when credential batch is issued before key is added" in {
      val keys = ec.generateKeyPair()
      val keyData = KeyData(publicKey = keys.publicKey, addedOn = now, revokedOn = None)
      val batchData = BatchData(issuedOn = before, revokedOn = None)
      val signedCredential = unsignedCredential.sign(keys.privateKey)
      val (root, proof) = rootAndProofFor(signedCredential)
      val revokedAt: Option[TimestampInfo] = None

      val e = PrismCredentialVerification
        .verify(
          keyData,
          batchData,
          revokedAt,
          root,
          proof,
          signedCredential
        )
        .invalid
      e.size must be(1)
      e.toList must contain(KeyWasNotValid(keyData.addedOn, batchData.issuedOn))
    }

    "return proper validation error when key is revoked before credential batch is issued" in {
      val keys = ec.generateKeyPair()
      val keyData = KeyData(publicKey = keys.publicKey, addedOn = before, revokedOn = Some(now))
      val batchData = BatchData(issuedOn = after, revokedOn = None)
      val signedCredential = unsignedCredential.sign(keys.privateKey)
      val (root, proof) = rootAndProofFor(signedCredential)
      val revokedAt: Option[TimestampInfo] = None

      val e = PrismCredentialVerification
        .verify(
          keyData,
          batchData,
          revokedAt,
          root,
          proof,
          signedCredential
        )
        .invalid
      e.size must be(1)
      e.toList must contain(KeyWasRevoked(batchData.issuedOn, keyData.revokedOn.value))
    }

    "return proper validation error when signature is invalid" in {
      val keys = ec.generateKeyPair()
      val keyData = KeyData(publicKey = keys.publicKey, addedOn = before, revokedOn = None)
      val batchData = BatchData(issuedOn = now, revokedOn = None)
      // Sign with different key
      val signedCredential = unsignedCredential.sign(ec.generateKeyPair().privateKey)
      val (root, proof) = rootAndProofFor(signedCredential)
      val revokedAt: Option[TimestampInfo] = None

      val e = PrismCredentialVerification
        .verify(
          keyData,
          batchData,
          revokedAt,
          root,
          proof,
          signedCredential
        )
        .invalid
      e.size must be(1)
      e.toList must contain(InvalidSignature)
    }

    "return proper validation error when key was added after the credential batch was issued AND the credential batch was revoked" in {
      val keys = ec.generateKeyPair()
      val keyData = KeyData(publicKey = keys.publicKey, addedOn = now, revokedOn = Some(after))
      // note that the key was revoked AFTER the credential is issued, this leads to not return a KeyWasRevoked
      val batchData = BatchData(issuedOn = before, revokedOn = Some(after))
      val signedCredential = unsignedCredential.sign(keys.privateKey)
      val (root, proof) = rootAndProofFor(signedCredential)
      val revokedAt: Option[TimestampInfo] = None

      val e = PrismCredentialVerification
        .verify(
          keyData,
          batchData,
          revokedAt,
          root,
          proof,
          signedCredential
        )
        .invalid
      e.size must be(2)
      e.toList must contain(KeyWasNotValid(keyData.addedOn, batchData.issuedOn))
      e.toList must contain(BatchWasRevoked(batchData.revokedOn.value))
    }

    "return proper validation error when key was revoked before the credential batch is issued AND the credential batch was revoked" in {
      val keys = ec.generateKeyPair()
      val keyData = KeyData(publicKey = keys.publicKey, addedOn = before, revokedOn = Some(before))
      // note that the key was revoked BEFORE the credential is issued, this leads to return a KeyWasRevoked
      val batchData = BatchData(issuedOn = now, revokedOn = Some(after))
      val signedCredential = unsignedCredential.sign(keys.privateKey)
      val (root, proof) = rootAndProofFor(signedCredential)
      val revokedAt: Option[TimestampInfo] = None

      val e = PrismCredentialVerification
        .verify(
          keyData,
          batchData,
          revokedAt,
          root,
          proof,
          signedCredential
        )
        .invalid
      e.size must be(2)
      e.toList must contain(BatchWasRevoked(batchData.revokedOn.value))
      e.toList must contain(KeyWasRevoked(batchData.issuedOn, keyData.revokedOn.value))
    }

    "return proper validation error when key was revoked before the credential batch is issued AND the credential batch was revoked AND the signature was invalid" in {
      val keys = ec.generateKeyPair()
      val keyData = KeyData(publicKey = keys.publicKey, addedOn = before, revokedOn = Some(before))
      // note that the key was revoked BEFORE the credential is issued, this leads to return a KeyWasRevoked
      val batchData = BatchData(issuedOn = now, revokedOn = Some(after))
      // Sign with different key
      val signedCredential = unsignedCredential.sign(ec.generateKeyPair().privateKey)
      val (root, proof) = rootAndProofFor(signedCredential)
      val revokedAt: Option[TimestampInfo] = None

      val e = PrismCredentialVerification
        .verify(
          keyData,
          batchData,
          revokedAt,
          root,
          proof,
          signedCredential
        )
        .invalid
      e.size must be(3)
      e.toList must contain(BatchWasRevoked(batchData.revokedOn.value))
      e.toList must contain(KeyWasRevoked(batchData.issuedOn, keyData.revokedOn.value))
      e.toList must contain(InvalidSignature)
    }
  }
}
