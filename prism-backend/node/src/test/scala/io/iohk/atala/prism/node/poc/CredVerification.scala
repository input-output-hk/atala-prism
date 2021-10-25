package io.iohk.atala.prism.node.poc

import cats.data.{Validated, ValidatedNel}
import cats.implicits.{catsSyntaxTuple6Semigroupal, catsSyntaxValidatedId}
import io.iohk.atala.prism.credentials.PrismCredential
import io.iohk.atala.prism.crypto.{MerkleInclusionProof, MerkleRoot}
import io.iohk.atala.prism.api.CredentialBatches
import io.iohk.atala.prism.protos.models.TimestampInfo
import io.iohk.atala.prism.models.KeyData

object CredVerification {

  sealed trait VerificationError
  object VerificationError {
    case class CredentialWasRevoked(revokedOn: TimestampInfo) extends VerificationError
    case class BatchWasRevoked(revokedOn: TimestampInfo) extends VerificationError
    case object InvalidMerkleProof extends VerificationError
    case class KeyWasNotValid(
        keyAddedOn: TimestampInfo,
        credentialIssuedOn: TimestampInfo
    ) extends VerificationError
    case class KeyWasRevoked(
        credentialIssuedOn: TimestampInfo,
        keyRevokedOn: TimestampInfo
    ) extends VerificationError
    case object InvalidSignature extends VerificationError
  }

  case class BatchData(
      batchIssuanceDate: TimestampInfo,
      revocationDate: Option[TimestampInfo]
  )

  import VerificationError._

  private val valid = ().validNel[VerificationError]

  /** This method receives data retrieved from the node and the credential to verify and returns true if and only if the
    * credential is valid.
    *
    * We have some assumptions to call this method:
    *   1. The keyData is obtained from the PRISM node and corresponds to the key used to sign the credential 2. The
    *      batchData is obtained from the PRISM node and corresponds to the signedCredential parameter 3. The issuer DID
    *      is a trusted one 4. The credentialRevocationTime is obtained from the PRISM node and corresponds to the
    *      signedCredential parameter
    *
    * @param keyData
    *   the public key used to sign the credential and its addition and (optional) revocation timestamps
    * @param batchData
    *   the credential information extracted from the node
    * @param credentialRevocationTime
    *   the credential information extracted from the node
    * @param merkleRoot
    *   merkle root that represents the batch
    * @param inclusionProof
    *   merkle proof of inclusion that states that signedCredential is in the batch
    * @param signedCredential
    *   the credential to verify
    * @return
    *   a validation result
    */
  def verify(
      keyData: KeyData,
      batchData: BatchData,
      credentialRevocationTime: Option[TimestampInfo],
      merkleRoot: MerkleRoot,
      inclusionProof: MerkleInclusionProof,
      signedCredential: PrismCredential
  ): ValidatedNel[VerificationError, Unit] = {

    // Scala's type system is evil, so we need this type alias to currify things for the
    // compiler (see https://stackoverflow.com/questions/49865936/scala-cats-validated-value-mapn-is-not-a-member-of-validatednel-tuple)
    type ValidationResult[A] = ValidatedNel[VerificationError, A]

    // the credential batch is not revoked
    val credentialBatchNotRevoked: ValidationResult[Unit] =
      batchData.revocationDate.fold(valid) { revokedOn =>
        BatchWasRevoked(revokedOn = revokedOn).invalidNel
      }

    // the key was added before the credential was issued
    val keyAddedBeforeIssuance: ValidationResult[Unit] =
      Validated.condNel(
        keyData.addedOn occurredBefore batchData.batchIssuanceDate,
        (),
        KeyWasNotValid(
          keyAddedOn = keyData.addedOn,
          credentialIssuedOn = batchData.batchIssuanceDate
        )
      )

    // the key is not revoked or, the key was revoked after the credential was signed
    val keyWasStillValid: ValidationResult[Unit] = {
      keyData.revokedOn match {
        case None => // the key was not revoked
          valid
        case Some(revokedOn) =>
          if (batchData.batchIssuanceDate occurredBefore revokedOn) valid
          else
            KeyWasRevoked(
              credentialIssuedOn = batchData.batchIssuanceDate,
              keyRevokedOn = revokedOn
            ).invalidNel
      }
    }

    // the signature is valid
    val signatureIsValid: ValidationResult[Unit] =
      Validated.condNel(
        signedCredential.isValidSignature(keyData.issuingKey),
        (),
        InvalidSignature
      )

    val individualCredentialNotRevoked: ValidationResult[Unit] =
      credentialRevocationTime.fold(valid) { revokedOn =>
        CredentialWasRevoked(revokedOn).invalidNel
      }

    val merkleProofIsValid: ValidationResult[Unit] =
      Validated.condNel(
        CredentialBatches
          .verifyInclusion(signedCredential, merkleRoot, inclusionProof),
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
}
