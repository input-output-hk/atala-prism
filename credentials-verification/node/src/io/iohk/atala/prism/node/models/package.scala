package io.iohk.atala.prism.node

import java.time.Instant

import enumeratum.EnumEntry.UpperSnakecase
import enumeratum._
import io.iohk.atala.crypto.ECPublicKey
import io.iohk.atala.prism.crypto.SHA256Digest
import io.iohk.atala.prism.node.operations.TimestampInfo

package object models {

  sealed trait KeyUsage extends EnumEntry with UpperSnakecase {
    // TODO: ATA-2854: revert this to
    //             def canIssue: Boolean = this == KeyUsage.IssuingKey
    //       after implementing proper key usage in the wallet
    def canIssue: Boolean = this == KeyUsage.IssuingKey || this == KeyUsage.MasterKey
  }

  object KeyUsage extends Enum[KeyUsage] {
    val values = findValues

    case object MasterKey extends KeyUsage
    case object IssuingKey extends KeyUsage
    case object CommunicationKey extends KeyUsage
    case object AuthenticationKey extends KeyUsage

  }

  class DIDSuffix private (val suffix: String) extends AnyVal {
    override def toString = suffix
  }

  object DIDSuffix {
    def apply(didSuffix: String): DIDSuffix = {
      require(DIDSuffix.DID_SUFFIX_RE.pattern.matcher(didSuffix).matches(), s"invalid DID format: $didSuffix")

      new DIDSuffix(didSuffix)
    }

    def apply(digest: SHA256Digest): DIDSuffix = apply(digest.hexValue)

    val DID_SUFFIX_RE = "^[0-9a-f]{64}$".r
  }

  case class DIDPublicKey(didSuffix: DIDSuffix, keyId: String, keyUsage: KeyUsage, key: ECPublicKey)

  case class DIDData(didSuffix: DIDSuffix, keys: List[DIDPublicKey], lastOperation: SHA256Digest)

  class CredentialId private (val id: String) extends AnyVal

  object CredentialId {
    def apply(id: String): CredentialId = {
      require(DIDSuffix.DID_SUFFIX_RE.pattern.matcher(id).matches())

      new CredentialId(id)
    }

    def apply(digest: SHA256Digest): CredentialId = apply(digest.hexValue)

    val CREDENTIAL_ID_RE = "^[0-9a-f]{64}$".r
  }

  case class AtalaObject(
      objectId: SHA256Digest,
      objectTimestamp: Instant,
      sequenceNumber: Int,
      byteContent: Option[Array[Byte]],
      processed: Boolean
  )

  object nodeState {

    case class CredentialState(
        credentialId: CredentialId,
        issuerDIDSuffix: DIDSuffix,
        contentHash: SHA256Digest,
        issuedOn: TimestampInfo,
        revokedOn: Option[TimestampInfo] = None,
        lastOperation: SHA256Digest
    )

    case class DIDPublicKeyState(
        didSuffix: DIDSuffix,
        keyId: String,
        keyUsage: KeyUsage,
        key: ECPublicKey,
        addedOn: TimestampInfo,
        revokedOn: Option[TimestampInfo]
    )

    case class DIDDataState(
        didSuffix: DIDSuffix,
        keys: List[DIDPublicKeyState],
        lastOperation: SHA256Digest
    )
  }
}
