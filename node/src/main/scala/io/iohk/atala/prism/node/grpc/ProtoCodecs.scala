package io.iohk.atala.prism.node.grpc

import java.security.PublicKey
import com.google.protobuf.ByteString
import io.iohk.atala.prism.protos.models.TimestampInfo
import io.iohk.atala.prism.crypto.EC.{INSTANCE => EC}
import io.iohk.atala.prism.crypto.keys.ECPublicKey
import io.iohk.atala.prism.node.models.{DidSuffix, Ledger, PublicKeyData}
import io.iohk.atala.prism.protos.common_models
import io.iohk.atala.prism.node.models
import io.iohk.atala.prism.node.models.KeyUsage._
import io.iohk.atala.prism.node.models.nodeState.LedgerData
import io.iohk.atala.prism.protos.node_models
import io.iohk.atala.prism.node.utils.syntax._

import java.time.Instant

object ProtoCodecs {
  def toTimeStampInfoProto(
      timestampInfo: TimestampInfo
  ): node_models.TimestampInfo = {
    node_models
      .TimestampInfo()
      .withBlockTimestamp(
        Instant
          .ofEpochMilli(timestampInfo.getAtalaBlockTimestamp)
          .toProtoTimestamp
      )
      .withBlockSequenceNumber(timestampInfo.getAtalaBlockSequenceNumber)
      .withOperationSequenceNumber(timestampInfo.getOperationSequenceNumber)
  }

  def atalaOperationToDIDDataProto(
      didSuffix: DidSuffix,
      op: node_models.AtalaOperation
  ): node_models.DIDData = {
    node_models
      .DIDData()
      .withId(didSuffix.getValue)
      .withPublicKeys(
        op.getCreateDid.didData
          .getOrElse(throw new RuntimeException("DID document with no keys"))
          .publicKeys
      )
  }

  def toDIDDataProto(
      did: String,
      didDataState: models.nodeState.DIDDataState
  ): node_models.DIDData = {
    node_models
      .DIDData()
      .withId(did)
      .withPublicKeys(
        didDataState.keys.map(key =>
          toProtoPublicKey(
            key.keyId,
            toCompressedECKeyData(key.key),
            toProtoKeyUsage(key.keyUsage),
            toLedgerData(key.addedOn),
            key.revokedOn map toLedgerData
          )
        )
      )
      .withServices(didDataState.services.map(toProtoService))
      .withContext(didDataState.context)
  }

  def toProtoService(nodeStateService: models.nodeState.DIDServiceState): node_models.Service = {

    val protoService = node_models
      .Service()
      .withId(nodeStateService.id)
      .withType(nodeStateService.`type`)
      .withServiceEndpoint(nodeStateService.serviceEndpoints)
      .withAddedOn(toLedgerData(nodeStateService.addedOn))

    nodeStateService.revokedOn
      .map(toLedgerData)
      .fold(protoService)(revokedOn => protoService.withDeletedOn(revokedOn))
  }

  def toProtoPublicKey(
      id: String,
      compressedEcKeyData: node_models.CompressedECKeyData,
      keyUsage: node_models.KeyUsage,
      addedOn: node_models.LedgerData,
      revokedOn: Option[node_models.LedgerData]
  ): node_models.PublicKey = {
    val withoutRevKey = node_models
      .PublicKey()
      .withId(id)
      .withCompressedEcKeyData(compressedEcKeyData)
      .withUsage(keyUsage)
      .withAddedOn(addedOn)

    revokedOn.fold(withoutRevKey)(revTime => withoutRevKey.withRevokedOn(revTime))
  }

  def toCompressedECKeyData(key: PublicKeyData): node_models.CompressedECKeyData = {
    node_models
      .CompressedECKeyData()
      .withCurve(key.curveName)
      .withData(ByteString.copyFrom(key.compressedKey))
  }

  def toProtoKeyUsage(keyUsage: models.KeyUsage): node_models.KeyUsage = {
    keyUsage match {
      case MasterKey => node_models.KeyUsage.MASTER_KEY
      case IssuingKey => node_models.KeyUsage.ISSUING_KEY
      case KeyAgreementKey => node_models.KeyUsage.KEY_AGREEMENT_KEY
      case RevocationKey => node_models.KeyUsage.REVOCATION_KEY
      case AuthenticationKey => node_models.KeyUsage.AUTHENTICATION_KEY
      case CapabilityInvocationKey => node_models.KeyUsage.CAPABILITY_INVOCATION_KEY
      case CapabilityDelegationKey => node_models.KeyUsage.CAPABILITY_DELEGATION_KEY
    }
  }

  // TODO: Manage proper validations.
  //       This implies making default values for operation sequence number to be 1
  //       (it is currently 0). The block sequence number starts at 1 already.
  def fromTimestampInfoProto(
      timestampInfoProto: node_models.TimestampInfo
  ): TimestampInfo = {
    new TimestampInfo(
      timestampInfoProto.blockTimestamp
        .getOrElse(throw new RuntimeException("Missing timestamp"))
        .toInstant
        .toEpochMilli,
      timestampInfoProto.blockSequenceNumber,
      timestampInfoProto.operationSequenceNumber
    )
  }

  def fromProtoKey(protoKey: node_models.PublicKey): Option[ECPublicKey] = {
    for {
      maybeX <- protoKey.keyData.ecKeyData
      maybeY <- protoKey.keyData.ecKeyData
    } yield EC.toPublicKeyFromByteCoordinates(
      maybeX.x.toByteArray,
      maybeY.y.toByteArray
    )
  }

  def fromProtoKeyLegacy(protoKey: node_models.PublicKey): Option[PublicKey] =
    fromProtoKey(protoKey).map(_.getKey$prism_crypto)

  def toLedgerData(ledgerData: LedgerData): node_models.LedgerData = {
    node_models.LedgerData(
      ledger = toLedger(ledgerData.ledger),
      transactionId = ledgerData.transactionId.toString,
      timestampInfo = Some(toTimeStampInfoProto(ledgerData.timestampInfo))
    )
  }

  def toLedger(ledger: Ledger): common_models.Ledger = {
    ledger match {
      case Ledger.InMemory => common_models.Ledger.IN_MEMORY
      case Ledger.CardanoTestnet => common_models.Ledger.CARDANO_TESTNET
      case Ledger.CardanoMainnet => common_models.Ledger.CARDANO_MAINNET
      case _ =>
        throw new IllegalArgumentException(s"Unexpected ledger: $ledger")
    }
  }
}
