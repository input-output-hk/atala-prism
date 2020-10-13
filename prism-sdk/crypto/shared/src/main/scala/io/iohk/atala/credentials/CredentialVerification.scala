package io.iohk.atala.credentials

import cats.implicits._
import cats.data._
import io.iohk.atala.crypto.ECTrait

sealed trait VerificationError
object VerificationError {
  case class Revoked(revokedOn: TimestampInfo) extends VerificationError
  case class KeyWasNotValid(keyAddedOn: TimestampInfo, credentialIssuedOn: TimestampInfo) extends VerificationError
  case class KeyWasRevoked(credentialIssuedOn: TimestampInfo, keyRevokedOn: TimestampInfo) extends VerificationError
  case object InvalidSignature extends VerificationError
}

object CredentialVerification {

  import VerificationError._

  /** This method receives data retrieved from the node and the credential to verify and
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
    * @param signedCredential the credential to verify
    * @return true if and only if the credential is considered valid under the assumptions
    */
  def verifyCredential(
      keyData: KeyData,
      credentialData: CredentialData,
      signedCredential: SignedCredential
  )(implicit ec: ECTrait): ValidatedNel[VerificationError, Unit] = {

    // Scala's type system is evil, so we need this type alias to currify things for the
    // compiler (see https://stackoverflow.com/questions/49865936/scala-cats-validated-value-mapn-is-not-a-member-of-validatednel-tuple)
    type ValidationResult[A] = ValidatedNel[VerificationError, A]

    // the credential is not revoked
    val credentialNotRevoked: ValidationResult[Unit] =
      credentialData.revokedOn.fold(().validNel[VerificationError]) { revokedOn =>
        Revoked(revokedOn = revokedOn).invalidNel
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
        CredentialsCryptoSDKImpl.verifyCredentialSignature(signedCredential, keyData.publicKey),
        (),
        InvalidSignature
      )

    (credentialNotRevoked, keyAddedBeforeIssuance, keyWasStillValid, signatureIsValid).mapN {
      (_: Unit, _: Unit, _: Unit, _: Unit) => ()
    }
  }
}
