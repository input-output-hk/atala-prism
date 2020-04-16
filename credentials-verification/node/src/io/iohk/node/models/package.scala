package io.iohk.node

import java.security.PublicKey
import java.time.Instant

import enumeratum.EnumEntry.UpperSnakecase
import enumeratum._
import io.iohk.cvp.crypto.SHA256Digest
import io.iohk.node.operations.TimestampInfo

package object models {

  sealed trait KeyUsage extends EnumEntry with UpperSnakecase {
    def canIssue: Boolean = this == KeyUsage.IssuingKey
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
      require(DIDSuffix.DID_SUFFIX_RE.pattern.matcher(didSuffix).matches())

      new DIDSuffix(didSuffix)
    }

    def apply(digest: SHA256Digest): DIDSuffix = apply(digest.hexValue)

    val DID_SUFFIX_RE = "^[0-9a-f]{64}$".r
  }

  case class DIDPublicKey(didSuffix: DIDSuffix, keyId: String, keyUsage: KeyUsage, key: PublicKey)

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

  case class Credential(
    credentialId: CredentialId,
    issuerDIDSuffix: DIDSuffix,
    contentHash: SHA256Digest,
    lastOperation: SHA256Digest
  )

  case class AtalaObject(
      objectId: SHA256Digest,
      objectTimestamp: Instant,
      sequenceNumber: Int,
      blockHash: Option[SHA256Digest],
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
    ) {
      def toCredential: Credential = Credential(credentialId, issuerDIDSuffix, contentHash, lastOperation)
    }

    case class DIDPublicKeyState(
      didSuffix: DIDSuffix,
      keyId: String,
      keyUsage: KeyUsage,
      key: PublicKey,
      addedOn: TimestampInfo,
      revokedOn: Option[TimestampInfo]
    ) {
      def toDIDPublicKey: DIDPublicKey = DIDPublicKey(didSuffix, keyId, keyUsage, key)
    }

    case class DIDDataState(
      didSuffix: DIDSuffix,
      keys: List[DIDPublicKeyState],
      lastOperation: SHA256Digest
    ) {
      def toDIDData: DIDData = {
        DIDData(didSuffix, keys map { _.toDIDPublicKey }, lastOperation)
      }
    }

  }
}
