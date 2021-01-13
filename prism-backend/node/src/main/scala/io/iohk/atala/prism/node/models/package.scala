package io.iohk.atala.prism.node

import enumeratum.EnumEntry.UpperSnakecase
import enumeratum._
import io.iohk.atala.prism.credentials.{CredentialBatchId, TimestampInfo}
import io.iohk.atala.prism.crypto.MerkleTree.MerkleRoot
import io.iohk.atala.prism.crypto.{ECPublicKey, SHA256Digest}
import io.iohk.atala.prism.identity.DIDSuffix
import io.iohk.atala.prism.models.{Ledger, TransactionId, TransactionInfo}

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

  case class DIDPublicKey(didSuffix: DIDSuffix, keyId: String, keyUsage: KeyUsage, key: ECPublicKey)

  case class DIDData(didSuffix: DIDSuffix, keys: List[DIDPublicKey], lastOperation: SHA256Digest)

  class CredentialId private (val id: String) extends AnyVal

  object CredentialId {
    def apply(id: String): CredentialId = {
      require(CREDENTIAL_ID_RE.pattern.matcher(id).matches())

      new CredentialId(id)
    }

    def apply(digest: SHA256Digest): CredentialId = apply(digest.hexValue)

    val CREDENTIAL_ID_RE = "^[0-9a-f]{64}$".r
  }

  case class AtalaObject(
      objectId: AtalaObjectId,
      // Serialization of a io.iohk.atala.prism.protos.node_internal.AtalaObject
      byteContent: Array[Byte],
      // Whether the object has been processed (e.g., DIDs were recognized and stored in DB)
      processed: Boolean,
      // Blockchain transaction the object was first found in
      transaction: Option[TransactionInfo] = None
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

    case class CredentialBatchState(
        batchId: CredentialBatchId,
        issuerDIDSuffix: DIDSuffix,
        merkleRoot: MerkleRoot,
        issuedOn: LedgerData,
        revokedOn: Option[LedgerData] = None,
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

    case class LedgerData(
        transactionId: TransactionId,
        ledger: Ledger,
        timestampInfo: TimestampInfo
    )
  }
}
