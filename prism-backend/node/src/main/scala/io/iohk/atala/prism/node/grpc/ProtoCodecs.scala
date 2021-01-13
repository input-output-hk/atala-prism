package io.iohk.atala.prism.node.grpc

import java.security.PublicKey
import java.time.Instant

import com.google.protobuf.ByteString
import io.iohk.atala.prism.credentials.TimestampInfo
import io.iohk.atala.prism.crypto.{EC, ECConfig, ECPublicKey, JvmECPublicKey}
import io.iohk.atala.prism.models.{ProtoCodecs => CommonProtoCodecs}
import io.iohk.atala.prism.identity.DIDSuffix
import io.iohk.atala.prism.node.models
import io.iohk.atala.prism.node.models.KeyUsage.{AuthenticationKey, CommunicationKey, IssuingKey, MasterKey}
import io.iohk.atala.prism.node.models.nodeState.{CredentialState, LedgerData}
import io.iohk.atala.prism.protos.{node_api, node_models}

object ProtoCodecs {
  def toCredentialStateResponseProto(credentialState: CredentialState): node_api.GetCredentialStateResponse = {
    val c = node_api
      .GetCredentialStateResponse()
      .withIssuerDID(credentialState.issuerDIDSuffix.value)
      .withPublicationDate(toTimeStampInfoProto(credentialState.issuedOn))

    credentialState.revokedOn.fold(c)(toTimeStampInfoProto _ andThen c.withRevocationDate)
  }

  def toTimeStampInfoProto(timestampInfo: TimestampInfo): node_models.TimestampInfo = {
    node_models
      .TimestampInfo()
      .withBlockTimestamp(timestampInfo.atalaBlockTimestamp.toEpochMilli)
      .withBlockSequenceNumber(timestampInfo.atalaBlockSequenceNumber)
      .withOperationSequenceNumber(timestampInfo.operationSequenceNumber)
  }

  def atalaOperationToDIDDataProto(didSuffix: DIDSuffix, op: node_models.AtalaOperation): node_models.DIDData = {
    node_models
      .DIDData()
      .withId(didSuffix.value)
      .withPublicKeys(
        op.getCreateDid.didData
          .getOrElse(throw new RuntimeException("DID document with no keys"))
          .publicKeys
      )
  }

  def toDIDDataProto(did: String, didDataState: models.nodeState.DIDDataState): node_models.DIDData = {
    node_models
      .DIDData()
      .withId(did)
      .withPublicKeys(
        didDataState.keys.map(key =>
          toProtoPublicKey(
            key.keyId,
            toECKeyData(key.key),
            toProtoKeyUsage(key.keyUsage),
            toTimeStampInfoProto(key.addedOn),
            key.revokedOn map toTimeStampInfoProto
          )
        )
      )
  }

  def toProtoPublicKey(
      id: String,
      ecKeyData: node_models.ECKeyData,
      keyUsage: node_models.KeyUsage,
      addedOn: node_models.TimestampInfo,
      revokedOn: Option[node_models.TimestampInfo]
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
      .withCurve(ECConfig.CURVE_NAME)
      .withX(ByteString.copyFrom(point.x.toByteArray))
      .withY(ByteString.copyFrom(point.y.toByteArray))
  }

  def toProtoKeyUsage(keyUsage: models.KeyUsage): node_models.KeyUsage = {
    keyUsage match {
      case MasterKey => node_models.KeyUsage.MASTER_KEY
      case IssuingKey => node_models.KeyUsage.ISSUING_KEY
      case CommunicationKey => node_models.KeyUsage.COMMUNICATION_KEY
      case AuthenticationKey => node_models.KeyUsage.AUTHENTICATION_KEY
    }
  }

  // TODO: Manage proper validations.
  //       This implies making default values for operation sequence number to be 1
  //       (it is currently 0). The block sequence number starts at 1 already.
  def fromTimestampInfoProto(timestampInfoProto: node_models.TimestampInfo): TimestampInfo = {
    TimestampInfo(
      Instant.ofEpochMilli(timestampInfoProto.blockTimestamp),
      timestampInfoProto.blockSequenceNumber,
      timestampInfoProto.operationSequenceNumber
    )
  }

  def fromProtoKey(protoKey: node_models.PublicKey): Option[ECPublicKey] = {
    for {
      maybeX <- protoKey.keyData.ecKeyData
      maybeY <- protoKey.keyData.ecKeyData
    } yield EC.toPublicKey(maybeX.x.toByteArray, maybeY.y.toByteArray)
  }

  def fromProtoKeyLegacy(protoKey: node_models.PublicKey): Option[PublicKey] = {
    fromProtoKey(protoKey).map {
      case jvmECPublicKey: JvmECPublicKey => jvmECPublicKey.key
    }
  }

  def toLedgerData(ledgerData: LedgerData): node_models.LedgerData = {
    node_models.LedgerData(
      ledger = CommonProtoCodecs.toLedger(ledgerData.ledger),
      transactionId = ledgerData.transactionId.toString,
      timestampInfo = Some(toTimeStampInfoProto(ledgerData.timestampInfo))
    )
  }
}
