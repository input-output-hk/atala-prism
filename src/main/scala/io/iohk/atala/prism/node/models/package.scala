package io.iohk.atala.prism.node

import derevo.derive
import enumeratum.EnumEntry.UpperSnakecase
import enumeratum._
import io.iohk.atala.prism.node.crypto.CryptoUtils.Sha256Hash
import io.iohk.atala.prism.protos.node_models
import tofu.logging.derivation.loggable

import java.time.Instant

package object models {
  sealed trait KeyUsage extends EnumEntry with UpperSnakecase {
    def canIssue: Boolean = this == KeyUsage.IssuingKey
    def canRevoke: Boolean = this == KeyUsage.RevocationKey

    def toProto: node_models.KeyUsage

    def derivationIndex: Int

    def name: String =
      if (toProto.name.nonEmpty) toProto.name else throw new IllegalStateException("Key usage must have name")

  }

  object KeyUsage extends Enum[KeyUsage] {
    val values = findValues

    case object MasterKey extends KeyUsage {
      override def toProto: node_models.KeyUsage = node_models.KeyUsage.MASTER_KEY
      override def derivationIndex: Int = 0
    }
    case object IssuingKey extends KeyUsage {
      override def toProto: node_models.KeyUsage = node_models.KeyUsage.ISSUING_KEY
      override def derivationIndex: Int = 1
    }
    case object KeyAgreementKey extends KeyUsage {
      override def toProto: node_models.KeyUsage = node_models.KeyUsage.KEY_AGREEMENT_KEY
      override def derivationIndex: Int = 2
    }
    case object RevocationKey extends KeyUsage {
      override def toProto: node_models.KeyUsage = node_models.KeyUsage.REVOCATION_KEY
      override def derivationIndex: Int = 3
    }
    case object AuthenticationKey extends KeyUsage {
      override def toProto: node_models.KeyUsage = node_models.KeyUsage.AUTHENTICATION_KEY
      override def derivationIndex: Int = 4
    }
    case object CapabilityInvocationKey extends KeyUsage {
      override def toProto: node_models.KeyUsage = node_models.KeyUsage.CAPABILITY_INVOCATION_KEY
      override def derivationIndex: Int = 5
    }
    case object CapabilityDelegationKey extends KeyUsage {
      override def toProto: node_models.KeyUsage = node_models.KeyUsage.CAPABILITY_DELEGATION_KEY
      override def derivationIndex: Int = 6

    }

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
      key: PublicKeyData
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
      lastOperation: Sha256Hash
  )

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

  case class PublicKeyData(
      curveName: String,
      compressedKey: Vector[Byte]
  )

  object nodeState {

    case class DIDPublicKeyState(
        didSuffix: DidSuffix,
        keyId: String,
        keyUsage: KeyUsage,
        key: PublicKeyData,
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
        lastOperation: Sha256Hash
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
