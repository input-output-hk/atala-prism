package io.iohk.atala.prism.credentials.japi

import java.util.Optional

import cats.data.Validated.{Invalid, Valid}
import io.iohk.atala.prism.credentials.VerificationError
import io.iohk.atala.prism.credentials.VerificationError.{
  InvalidSignature,
  KeyWasNotValid,
  KeyWasRevoked,
  CredentialWasRevoked
}
import io.iohk.atala.prism.credentials.japi.verification.{VerificationResult, error => jvmError}
import io.iohk.atala.prism.credentials.japi.verification.error.{VerificationError => JvmVerificationError}
import io.iohk.atala.prism.credentials.{
  Base64URLCredential => JvmBase64URLCredential,
  Base64URLSignature => JvmBase64URLSignature,
  CredentialData => JvmCredentialData,
  CredentialVerification => JvmCredentialVerification,
  KeyData => JvmKeyData,
  SignedCredential => JvmSignedCredential,
  TimestampInfo => JvmTimestampInfo
}
import io.iohk.atala.prism.crypto.ECTrait
import io.iohk.atala.prism.crypto.japi.ECPublicKeyFacade

private[japi] class CredentialVerificationFacade(implicit val ec: ECTrait) extends CredentialVerification {

  override def verifyCredential(
      keyData: KeyData,
      credentialData: CredentialData,
      signedCredential: SignedCredential
  ): VerificationResult = {
    val publicKey = keyData.getPublicKey.asInstanceOf[ECPublicKeyFacade]

    val verificationResult = JvmCredentialVerification.verifyCredential(
      JvmKeyData(
        publicKey = publicKey.publicKey,
        addedOn = toJvmTimestampInfo(keyData.getAddedOn),
        revokedOn = toJvmTimestampInfo(keyData.getRevokedOn)
      ),
      JvmCredentialData(
        issuedOn = toJvmTimestampInfo(credentialData.getIssuedOn),
        revokedOn = toJvmTimestampInfo(credentialData.getRevokedOn)
      ),
      JvmSignedCredential(
        credential = JvmBase64URLCredential(signedCredential.getCredential.getValue),
        signature = JvmBase64URLSignature(signedCredential.getSignature.getValue)
      )
    )

    import scala.jdk.CollectionConverters._

    verificationResult match {
      case Valid(isValid) => new VerificationResult(List[JvmVerificationError]().asJava)
      case Invalid(errors) => new VerificationResult((errors.toList map toJvmVerificationError).asJava)
    }
  }

  private def toJvmVerificationError(err: VerificationError): JvmVerificationError =
    err match {
      case CredentialWasRevoked(revokedOn) => new jvmError.Revoked(toTimestampInfo(revokedOn))
      case KeyWasRevoked(credentialIssuedOn, keyRevokedOn) =>
        new jvmError.KeyWasRevoked(toTimestampInfo(credentialIssuedOn), toTimestampInfo(keyRevokedOn))
      case KeyWasNotValid(keyAddedOn, credentialIssuedOn) =>
        new jvmError.KeyWasNotValid(toTimestampInfo(keyAddedOn), toTimestampInfo(credentialIssuedOn))
      case InvalidSignature =>
        new jvmError.InvalidSignature
      case other => throw new NotImplementedError(s"$other error is not implemented")
    }

  private def toTimestampInfo(timestampInfo: JvmTimestampInfo): TimestampInfo =
    new TimestampInfo(
      timestampInfo.atalaBlockTimestamp,
      timestampInfo.atalaBlockSequenceNumber,
      timestampInfo.operationSequenceNumber
    )

  private def toJvmTimestampInfo(timestampInfo: Optional[TimestampInfo]): Option[JvmTimestampInfo] = {
    if (timestampInfo.isPresent) {
      Some(toJvmTimestampInfo(timestampInfo.get))
    } else {
      None
    }
  }

  private def toJvmTimestampInfo(timestampInfo: TimestampInfo): JvmTimestampInfo = {
    JvmTimestampInfo(
      timestampInfo.getAtalaBlockTimestamp,
      timestampInfo.getAtalaBlockSequenceNumber,
      timestampInfo.getOperationSequenceNumber
    )
  }
}
