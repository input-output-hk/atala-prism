package io.iohk.atala.prism.node

import enumeratum.EnumEntry.UpperSnakecase
import enumeratum._
import io.iohk.atala.prism.connector.AtalaOperationId
import io.iohk.atala.prism.kotlin.credentials.CredentialBatchId
import io.iohk.atala.prism.kotlin.crypto.{MerkleRoot, Sha256Digest}
import io.iohk.atala.prism.kotlin.crypto.keys.ECPublicKey
import io.iohk.atala.prism.kotlin.protos.models.TimestampInfo
import io.iohk.atala.prism.models.{DIDSuffix, Ledger, TransactionId}

import java.time.Instant
import scala.util.matching.Regex

package object models {
  sealed trait KeyUsage extends EnumEntry with UpperSnakecase {
    def canIssue: Boolean = this == KeyUsage.IssuingKey
    def canRevoke: Boolean = this == KeyUsage.RevocationKey
  }

  object KeyUsage extends Enum[KeyUsage] {
    val values = findValues

    case object MasterKey extends KeyUsage
    case object IssuingKey extends KeyUsage
    case object CommunicationKey extends KeyUsage
    case object RevocationKey extends KeyUsage
    case object AuthenticationKey extends KeyUsage

  }

  val ATALA_OBJECT_VERSION: String = "1.0"

  sealed trait AtalaObjectStatus extends EnumEntry with UpperSnakecase
  object AtalaObjectStatus extends Enum[AtalaObjectStatus] {
    val values = findValues

    case object Pending extends AtalaObjectStatus
    case object Merged extends AtalaObjectStatus
    case object Processed extends AtalaObjectStatus
  }

  case class DIDPublicKey(didSuffix: DIDSuffix, keyId: String, keyUsage: KeyUsage, key: ECPublicKey)

  case class DIDData(didSuffix: DIDSuffix, keys: List[DIDPublicKey], lastOperation: Sha256Digest)

  class CredentialId private (val id: String) extends AnyVal

  object CredentialId {
    def apply(id: String): CredentialId = {
      require(CREDENTIAL_ID_RE.pattern.matcher(id).matches(), s"invalid credential id: $id")

      new CredentialId(id)
    }

    def apply(digest: Sha256Digest): CredentialId = apply(digest.getHexValue)

    val CREDENTIAL_ID_RE: Regex = "^[0-9a-f]{64}$".r
  }

  case class AtalaOperationInfo(
      operationId: AtalaOperationId,
      objectId: AtalaObjectId,
      operationStatus: AtalaOperationStatus,
      transactionSubmissionStatus: Option[AtalaObjectTransactionSubmissionStatus] = None,
      transactionId: Option[TransactionId] = None
  )

  sealed trait AtalaOperationStatus extends EnumEntry with UpperSnakecase

  object AtalaOperationStatus extends Enum[AtalaOperationStatus] {
    val values = findValues

    case object UNKNOWN extends AtalaOperationStatus
    case object RECEIVED extends AtalaOperationStatus // Received by PRISM
    case object APPLIED extends AtalaOperationStatus // Confirmed and applied to PRISM state
    case object REJECTED extends AtalaOperationStatus // Confirmed, but rejected by PRISM
  }

  object nodeState {

    case class CredentialBatchState(
        batchId: CredentialBatchId,
        issuerDIDSuffix: DIDSuffix,
        merkleRoot: MerkleRoot,
        issuedOn: LedgerData,
        revokedOn: Option[LedgerData] = None,
        lastOperation: Sha256Digest
    )

    case class DIDPublicKeyState(
        didSuffix: DIDSuffix,
        keyId: String,
        keyUsage: KeyUsage,
        key: ECPublicKey,
        addedOn: LedgerData,
        revokedOn: Option[LedgerData]
    )

    case class DIDDataState(
        didSuffix: DIDSuffix,
        keys: List[DIDPublicKeyState],
        lastOperation: Sha256Digest
    )

    case class LedgerData(
        transactionId: TransactionId,
        ledger: Ledger,
        timestampInfo: TimestampInfo
    )

    def getLastSyncedTimestampFromMaybe(maybeLastSyncedBlockTimestamp: Option[String]): Instant = {
      val lastSyncedBlockTimestamp =
        maybeLastSyncedBlockTimestamp
          .flatMap(_.toLongOption)
          .getOrElse(0L)
      Instant.ofEpochMilli(lastSyncedBlockTimestamp)
    }
  }
}
