package io.iohk.atala.prism.credentials.japi

import io.iohk.atala.prism.credentials.japi.verification.VerificationResult
import io.iohk.atala.prism.credentials.japi.verification.error.VerificationError
import io.iohk.atala.prism.credentials.{
  CredentialData => SCredentialData,
  KeyData => SKeyData,
  PrismCredentialVerification => SPrismCredentialVerification,
  TimestampInfo => STimestampInfo,
  VerificationError => SVerificationError
}
import io.iohk.atala.prism.crypto.ECTrait
import io.iohk.atala.prism.crypto.japi.{EC, ECFacade, ECPublicKeyFacade}

import scala.jdk.CollectionConverters._

private[japi] object PrismCredentialVerificationFacade {

  def convertTimestampInfo(info: TimestampInfo): STimestampInfo = {
    STimestampInfo(info.getAtalaBlockTimestamp, info.getAtalaBlockSequenceNumber, info.getOperationSequenceNumber)
  }

  def convertTimestampInfo(info: STimestampInfo): TimestampInfo = {
    new TimestampInfo(info.atalaBlockTimestamp, info.atalaBlockSequenceNumber, info.operationSequenceNumber)
  }

  def convertError(error: SVerificationError): VerificationError = {
    error match {
      case SVerificationError.InvalidSignature => new verification.error.InvalidSignature
      case SVerificationError.KeyWasNotValid(keyAddedOn, credentialIssuedOn) =>
        new verification.error.KeyWasNotValid(
          convertTimestampInfo(keyAddedOn),
          convertTimestampInfo(credentialIssuedOn)
        )
      case SVerificationError.KeyWasRevoked(credentialIssuedOn, keyRevokedOn) =>
        new verification.error.KeyWasRevoked(
          convertTimestampInfo(credentialIssuedOn),
          convertTimestampInfo(keyRevokedOn)
        )
      case SVerificationError.Revoked(revokedOn) =>
        new verification.error.Revoked(convertTimestampInfo(revokedOn))
    }
  }

  def verify(keyData: KeyData, credentialData: CredentialData, credential: Credential, ec: EC): VerificationResult = {

    implicit val ecTrait: ECTrait = ECFacade.unwrap(ec)

    val ecCredential = credential match {
      case c: JsonBasedCredentialFacade[_] => c
      case c => throw new IllegalArgumentException(s"Cannot verify credential of type ${c.getClass.getName}")
    }

    val _keyData = SKeyData(
      ECPublicKeyFacade.unwrap(keyData.getPublicKey),
      convertTimestampInfo(keyData.getAddedOn),
      Option(keyData.getRevokedOn.orElse(null)).map(convertTimestampInfo)
    )
    val _credentialData = SCredentialData(
      convertTimestampInfo(credentialData.getIssuedOn),
      Option(credentialData.getRevokedOn.orElse(null)).map(convertTimestampInfo)
    )
    SPrismCredentialVerification
      .verify(_keyData, _credentialData, ecCredential.wrapped)
      .fold(
        { errors =>
          new VerificationResult(errors.map(convertError).toList.asJava)
        },
        _ => new VerificationResult(java.util.Collections.emptyList())
      )
  }
}
