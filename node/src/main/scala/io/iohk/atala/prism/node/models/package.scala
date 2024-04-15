package io.iohk.atala.prism.node

import derevo.derive
import enumeratum.EnumEntry.UpperSnakecase
import enumeratum._
import io.iohk.atala.prism.credentials.CredentialBatchId
import io.iohk.atala.prism.crypto.{MerkleRoot, Sha256Digest}
import io.iohk.atala.prism.protos.models.TimestampInfo
import io.iohk.atala.prism.models.{AtalaOperationId, DidSuffix, IdType, Ledger, TransactionId}
import io.iohk.atala.prism.protos.node_models
import tofu.logging.derivation.loggable

import java.time.Instant
import scala.util.matching.Regex
// import io.iohk.atala.prism.apollo.utils.KMMECPoint
import identus.apollo.MyPublicKey

package object models {
  sealed trait KeyUsage extends EnumEntry with UpperSnakecase {
    def canIssue: Boolean = this == KeyUsage.IssuingKey
    def canRevoke: Boolean = this == KeyUsage.RevocationKey
  }

  object KeyUsage extends Enum[KeyUsage] {
    val values = findValues

    case object MasterKey extends KeyUsage
    case object IssuingKey extends KeyUsage
    case object KeyAgreementKey extends KeyUsage
    case object RevocationKey extends KeyUsage
    case object AuthenticationKey extends KeyUsage
    case object CapabilityInvocationKey extends KeyUsage
    case object CapabilityDelegationKey extends KeyUsage

  }

  val ATALA_OBJECT_VERSION: String = "1.0"

  sealed trait AtalaObjectStatus extends EnumEntry with UpperSnakecase
  object AtalaObjectStatus extends Enum[AtalaObjectStatus] {
    val values = findValues

    case object Scheduled extends AtalaObjectStatus
    case object Pending extends AtalaObjectStatus
    case object Merged extends AtalaObjectStatus
    case object Processed extends AtalaObjectStatus
  }

  case class DIDPublicKey(
      didSuffix: DidSuffix,
      keyId: String,
      keyUsage: KeyUsage,
      key: MyPublicKey
  )

  case class DIDService(
      id: String,
      didSuffix: DidSuffix,
      `type`: String,
      serviceEndpoints: String
  )

  case class DIDData(
      didSuffix: DidSuffix,
      keys: List[DIDPublicKey],
      services: List[DIDService],
      context: List[String],
      lastOperation: Sha256Digest
  )

  class CredentialId private (val id: String) extends AnyVal

  object CredentialId {
    def apply(id: String): CredentialId = {
      require(
        CREDENTIAL_ID_RE.pattern.matcher(id).matches(),
        s"invalid credential id: $id"
      )

      new CredentialId(id)
    }

    def apply(digest: Sha256Digest): CredentialId = apply(digest.getHexValue)

    val CREDENTIAL_ID_RE: Regex = "^[0-9a-f]{64}$".r
  }

  @derive(loggable)
  case class AtalaOperationInfo(
      operationId: AtalaOperationId,
      objectId: AtalaObjectId,
      operationStatus: AtalaOperationStatus,
      operationStatusDetails: String,
      transactionSubmissionStatus: Option[
        AtalaObjectTransactionSubmissionStatus
      ] = None,
      transactionId: Option[TransactionId] = None
  )

  @derive(loggable)
  sealed trait AtalaOperationStatus extends EnumEntry with UpperSnakecase

  object AtalaOperationStatus extends Enum[AtalaOperationStatus] {
    val values = findValues

    case object UNKNOWN extends AtalaOperationStatus
    case object RECEIVED extends AtalaOperationStatus // Received by PRISM
    case object APPLIED extends AtalaOperationStatus // Confirmed and applied to PRISM state
    case object REJECTED extends AtalaOperationStatus // Confirmed, but rejected by PRISM
  }

  @derive(loggable)
  case class ProtocolVersion(major: Int, minor: Int) {
    override def toString: String = s"$major.$minor"

    def isFollowedBy(next: ProtocolVersion): Boolean =
      major == next.major && minor + 1 == next.minor ||
        major + 1 == next.major && next.minor == 0

    def toProto: node_models.ProtocolVersion =
      node_models.ProtocolVersion(majorVersion = major, minorVersion = minor)
  }

  object ProtocolVersion {
    // All existing so far protocol versions here
    val ProtocolVersion1_0: ProtocolVersion = ProtocolVersion(1, 0)
    val InitialProtocolVersion: ProtocolVersion = ProtocolVersion1_0
  }

  case class ProtocolVersionInfo(
      protocolVersion: ProtocolVersion,
      versionName: Option[String],
      effectiveSinceBlockIndex: Int
  )

  object ProtocolVersionInfo {
    val InitialProtocolVersionInfo: ProtocolVersionInfo =
      ProtocolVersionInfo(ProtocolVersion.InitialProtocolVersion, None, 0)
  }

  object nodeState {

    case class CredentialBatchState(
        batchId: CredentialBatchId,
        issuerDIDSuffix: DidSuffix,
        merkleRoot: MerkleRoot,
        issuedOn: LedgerData,
        revokedOn: Option[LedgerData] = None,
        lastOperation: Sha256Digest
    )

    case class DIDPublicKeyState(
        didSuffix: DidSuffix,
        keyId: String,
        keyUsage: KeyUsage,
        key: MyPublicKey,
        addedOn: LedgerData,
        revokedOn: Option[LedgerData]
    )

    case class DIDServiceState(
        serviceId: IdType,
        id: String,
        didSuffix: DidSuffix,
        `type`: String,
        serviceEndpoints: String,
        addedOn: LedgerData,
        revokedOn: Option[LedgerData]
    )

    case class DIDDataState(
        didSuffix: DidSuffix,
        keys: List[DIDPublicKeyState],
        services: List[DIDServiceState],
        context: List[String],
        lastOperation: Sha256Digest
    )

    case class LedgerData(
        transactionId: TransactionId,
        ledger: Ledger,
        timestampInfo: TimestampInfo
    )

    def getLastSyncedTimestampFromMaybe(
        maybeLastSyncedBlockTimestamp: Option[String]
    ): Instant = {
      val lastSyncedBlockTimestamp =
        maybeLastSyncedBlockTimestamp
          .flatMap(_.toLongOption)
          .getOrElse(0L)
      Instant.ofEpochMilli(lastSyncedBlockTimestamp)
    }
  }
}
