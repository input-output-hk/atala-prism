package io.iohk.atala.credentials.japi

import java.util.Optional

import io.iohk.atala.credentials.{
  Base64URLCredential => JvmBase64URLCredential,
  Base64URLSignature => JvmBase64URLSignature,
  CredentialData => JvmCredentialData,
  CredentialVerification => JvmCredentialVerification,
  KeyData => JvmKeyData,
  SignedCredential => JvmSignedCredential,
  TimestampInfo => JvmTimestampInfo
}
import io.iohk.atala.crypto.ECTrait
import io.iohk.atala.crypto.japi.ECPublicKeyFacade

class CredentialVerificationFacade(implicit val ec: ECTrait) extends CredentialVerification {

  override def verifyCredential(
      keyData: KeyData,
      credentialData: CredentialData,
      signedCredential: SignedCredential
  ): Boolean = {
    val publicKey = keyData.getPublicKey.asInstanceOf[ECPublicKeyFacade]

    JvmCredentialVerification.verifyCredential(
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
  }

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
