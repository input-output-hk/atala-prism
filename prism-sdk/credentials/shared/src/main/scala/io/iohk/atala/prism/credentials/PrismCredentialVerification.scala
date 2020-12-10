package io.iohk.atala.prism.credentials

import cats.implicits._
import cats.data._
import io.iohk.atala.prism.crypto.ECTrait
import io.iohk.atala.prism.crypto.MerkleTree.{MerkleInclusionProof, MerkleRoot}

sealed trait VerificationError
object VerificationError {
  case class CredentialWasRevoked(revokedOn: TimestampInfo) extends VerificationError
  case class BatchWasRevoked(revokedOn: TimestampInfo) extends VerificationError
  case object InvalidMerkleProof extends VerificationError
  case class KeyWasNotValid(keyAddedOn: TimestampInfo, credentialIssuedOn: TimestampInfo) extends VerificationError
  case class KeyWasRevoked(credentialIssuedOn: TimestampInfo, keyRevokedOn: TimestampInfo) extends VerificationError
  case object InvalidSignature extends VerificationError
}

object PrismCredentialVerification {

  import VerificationError._

  private val valid = ().validNel[VerificationError]

  /** This method receives data retrieved from the node and the credential to verify and
    * returns true if and only if the credential is valid.
    *
    * We have some assumptions to call this method:
    * 1. The keyData is obtained from the PRISM node and corresponds to the key used to sign the credential
    * 2. The batchData is obtained from the PRISM node and corresponds to the signedCredential parameter
    * 3. The issuer DID is a trusted one
    * 4. The credentialRevocationTime is obtained from the PRISM node and corresponds to the signedCredential parameter
    *
    * @param keyData the public key used to sign the credential and its addition and (optional)
    *                revocation timestamps
    * @param batchData the credential information extracted from the node
    * @param credentialRevocationTime the credential information extracted from the node
    * @param merkleRoot merkle root that represents the batch
    * @param inclusionProof merkle proof of inclusion that states that signedCredential is in the batch
    * @param signedCredential the credential to verify
    * @return a validation result
    */
  def verify(
      keyData: KeyData,
      batchData: BatchData,
      credentialRevocationTime: Option[TimestampInfo],
      merkleRoot: MerkleRoot,
      inclusionProof: MerkleInclusionProof,
      signedCredential: Credential
  )(implicit ec: ECTrait): ValidatedNel[VerificationError, Unit] = {

    // Scala's type system is evil, so we need this type alias to currify things for the
    // compiler (see https://stackoverflow.com/questions/49865936/scala-cats-validated-value-mapn-is-not-a-member-of-validatednel-tuple)
    type ValidationResult[A] = ValidatedNel[VerificationError, A]

    // the credential batch is not revoked
    val credentialBatchNotRevoked: ValidationResult[Unit] =
      batchData.revokedOn.fold(valid) { revokedOn =>
        BatchWasRevoked(revokedOn = revokedOn).invalidNel
      }

    // the key was added before the credential was issued
    val keyAddedBeforeIssuance: ValidationResult[Unit] =
      Validated.condNel(
        keyData.addedOn occurredBefore batchData.issuedOn,
        (),
        KeyWasNotValid(keyAddedOn = keyData.addedOn, credentialIssuedOn = batchData.issuedOn)
      )

    // the key is not revoked or, the key was revoked after the credential was signed
    val keyWasStillValid: ValidationResult[Unit] = {
      keyData.revokedOn match {
        case None => // the key was not revoked
          valid
        case Some(revokedOn) =>
          if (batchData.issuedOn occurredBefore revokedOn) valid
          else KeyWasRevoked(credentialIssuedOn = batchData.issuedOn, keyRevokedOn = revokedOn).invalidNel
      }
    }

    // the signature is valid
    val signatureIsValid: ValidationResult[Unit] =
      Validated.condNel(
        signedCredential.isValidSignature(keyData.publicKey),
        (),
        InvalidSignature
      )

    val individualCredentialNotRevoked: ValidationResult[Unit] =
      credentialRevocationTime.fold(valid) { revokedOn =>
        CredentialWasRevoked(revokedOn).invalidNel
      }

    val merkleProofIsValid: ValidationResult[Unit] =
      Validated.condNel(
        CredentialBatches.verifyInclusion(signedCredential, merkleRoot, inclusionProof),
        (),
        InvalidMerkleProof
      )

    (
      credentialBatchNotRevoked,
      keyAddedBeforeIssuance,
      keyWasStillValid,
      signatureIsValid,
      individualCredentialNotRevoked,
      merkleProofIsValid
    ).mapN { (_: Unit, _: Unit, _: Unit, _: Unit, _: Unit, _: Unit) =>
      ()
    }
  }

  /**
    * This method receives data retrieved from the node and the credential to verify and
    * returns true if and only if the credential is valid.
    *
    * We have some assumptions to call this method:
    * 1. The keyData is obtained from the PRISM node and corresponds to the key used to sign the credential
    * 2. The credentialData is obtained from the PRISM node and corresponds to the signedCredential parameter
    * 3. The issuer DID is a trusted one
    *
    * @param keyData the public key used to sign the credential and its addition and (optional)
    *                revocation timestamps
    * @param credentialData the credential information extracted from the node
    * @param credential the credential to verify
    */
  def verify(
      keyData: KeyData,
      credentialData: CredentialData,
      credential: Credential
  )(implicit ec: ECTrait): ValidatedNel[VerificationError, Unit] = {

    // Scala's type system is evil, so we need this type alias to currify things for the
    // compiler (see https://stackoverflow.com/questions/49865936/scala-cats-validated-value-mapn-is-not-a-member-of-validatednel-tuple)
    type ValidationResult[A] = ValidatedNel[VerificationError, A]

    // the credential is not revoked
    val credentialNotRevoked: ValidationResult[Unit] =
      credentialData.revokedOn.fold(().validNel[VerificationError]) { revokedOn =>
        CredentialWasRevoked(revokedOn = revokedOn).invalidNel
      }

    // the key was added before the credential was issued
    val keyAddedBeforeIssuance: ValidationResult[Unit] =
      Validated.condNel(
        keyData.addedOn occurredBefore credentialData.issuedOn,
        (),
        KeyWasNotValid(keyAddedOn = keyData.addedOn, credentialIssuedOn = credentialData.issuedOn)
      )

    // the key is not revoked or, the key was revoked after the credential was signed
    val keyWasStillValid: ValidationResult[Unit] = {
      keyData.revokedOn match {
        case None => // the key was not revoked
          ().validNel[VerificationError]
        case Some(revokedOn) =>
          if (credentialData.issuedOn occurredBefore revokedOn) ().validNel[VerificationError]
          else KeyWasRevoked(credentialIssuedOn = credentialData.issuedOn, keyRevokedOn = revokedOn).invalidNel
      }
    }

    // the signature is valid
    val signatureIsValid: ValidationResult[Unit] =
      Validated.condNel(
        credential.isValidSignature(keyData.publicKey),
        (),
        InvalidSignature
      )

    (credentialNotRevoked, keyAddedBeforeIssuance, keyWasStillValid, signatureIsValid).mapN {
      (_: Unit, _: Unit, _: Unit, _: Unit) => ()
    }
  }

}
