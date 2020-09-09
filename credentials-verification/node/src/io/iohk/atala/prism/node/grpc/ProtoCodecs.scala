package io.iohk.atala.prism.node.grpc

import java.security.PublicKey
import java.time.Instant

import com.google.protobuf.ByteString
import io.iohk.atala.crypto.{EC, ECConfig, ECPublicKey}
import io.iohk.atala.prism.crypto.ECKeys
import io.iohk.atala.prism.node.models
import io.iohk.atala.prism.node.models.KeyUsage.{AuthenticationKey, CommunicationKey, IssuingKey, MasterKey}
import io.iohk.atala.prism.node.models.nodeState.CredentialState
import io.iohk.atala.prism.node.operations.TimestampInfo
import io.iohk.atala.prism.protos.{node_api, node_models}

object ProtoCodecs {
  def toCredentialStateResponseProto(credentialState: CredentialState): node_api.GetCredentialStateResponse = {
    val c = node_api
      .GetCredentialStateResponse()
      .withIssuerDID(credentialState.issuerDIDSuffix.suffix)
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

  def toDIDDataProto(didDataState: models.nodeState.DIDDataState): node_models.DIDData = {
    node_models
      .DIDData()
      .withId(didDataState.didSuffix.suffix)
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

  // TODO: Remove this once the `io.iohk.atala.prism.node.poc` package can be removed or migrated.
  def fromProtoKeyLegacy(protoKey: node_models.PublicKey): Option[PublicKey] = {
    for {
      maybeX <- protoKey.keyData.ecKeyData
      maybeY <- protoKey.keyData.ecKeyData
    } yield ECKeys.toPublicKey(maybeX.x.toByteArray, maybeY.y.toByteArray)
  }
}
