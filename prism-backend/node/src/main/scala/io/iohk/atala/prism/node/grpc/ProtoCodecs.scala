package io.iohk.atala.prism.node.grpc

import java.security.PublicKey
import com.google.protobuf.ByteString
import io.iohk.atala.prism.protos.models.TimestampInfo
import io.iohk.atala.prism.crypto.EC.{INSTANCE => EC}
import io.iohk.atala.prism.crypto.keys.ECPublicKey
import io.iohk.atala.prism.crypto.ECConfig.{INSTANCE => ECConfig}
import io.iohk.atala.prism.models.{DidSuffix, Ledger}
import io.iohk.atala.prism.protos.common_models
import io.iohk.atala.prism.node.models
import io.iohk.atala.prism.node.models.KeyUsage.{
  AuthenticationKey,
  CommunicationKey,
  IssuingKey,
  MasterKey,
  RevocationKey
}
import io.iohk.atala.prism.node.models.nodeState.LedgerData
import io.iohk.atala.prism.protos.node_models
import io.iohk.atala.prism.utils.syntax._

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
            toECKeyData(key.key),
            toProtoKeyUsage(key.keyUsage),
            toLedgerData(key.addedOn),
            key.revokedOn map toLedgerData
          )
        )
      )
      .withServices(didDataState.services.map(toProtoService))
  }

  def toProtoService(nodeStateService: models.nodeState.DIDServiceState): node_models.Service = {

    val protoService = node_models
      .Service()
      .withId(nodeStateService.id)
      .withType(nodeStateService.`type`)
      .withServiceEndpoint(nodeStateService.serviceEndpoints.map(_.url))
      .withAddedOn(toLedgerData(nodeStateService.addedOn))

    nodeStateService.revokedOn
      .map(toLedgerData)
      .fold(protoService)(revokedOn => protoService.withDeletedOn(revokedOn))
  }

  def toProtoPublicKey(
      id: String,
      ecKeyData: node_models.ECKeyData,
      keyUsage: node_models.KeyUsage,
      addedOn: node_models.LedgerData,
      revokedOn: Option[node_models.LedgerData]
  ): node_models.PublicKey = {
    val withoutRevKey = node_models
      .PublicKey()
      .withId(id)
      .withEcKeyData(ecKeyData)
      .withUsage(keyUsage)
      .withAddedOn(addedOn)

    revokedOn.fold(withoutRevKey)(revTime => withoutRevKey.withRevokedOn(revTime))
  }

  def toECKeyData(key: ECPublicKey): node_models.ECKeyData = {
    val point = key.getCurvePoint
    node_models
      .ECKeyData()
      .withCurve(ECConfig.getCURVE_NAME)
      .withX(ByteString.copyFrom(point.getX.bytes()))
      .withY(ByteString.copyFrom(point.getY.bytes()))
  }

  def toProtoKeyUsage(keyUsage: models.KeyUsage): node_models.KeyUsage = {
    keyUsage match {
      case MasterKey => node_models.KeyUsage.MASTER_KEY
      case IssuingKey => node_models.KeyUsage.ISSUING_KEY
      case CommunicationKey => node_models.KeyUsage.COMMUNICATION_KEY
      case RevocationKey => node_models.KeyUsage.REVOCATION_KEY
      case AuthenticationKey => node_models.KeyUsage.AUTHENTICATION_KEY
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
