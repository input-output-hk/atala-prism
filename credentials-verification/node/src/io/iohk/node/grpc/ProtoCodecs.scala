package io.iohk.node.grpc

import java.security.PublicKey
import java.time.Instant

import com.google.protobuf.ByteString
import io.iohk.cvp.crypto.ECKeys
import io.iohk.node.models
import io.iohk.node.models.KeyUsage.{AuthenticationKey, CommunicationKey, IssuingKey, MasterKey}
import io.iohk.node.models.nodeState.CredentialState
import io.iohk.node.operations.TimestampInfo
import io.iohk.prism.protos.{node_api, node_models}

object ProtoCodecs {
  def toCredentialStateResponseProto(credentialState: CredentialState): node_api.GetCredentialStateResponse = {
    val c = node_api
      .GetCredentialStateResponse()
      .withIssuerDID(credentialState.issuerDIDSuffix.suffix)
      .withPublicationDate(toTimeStampInfoProto(credentialState.issuedOn))

    credentialState.revokedOn.fold(c)(toTimeStampInfoProto _ andThen c.withRevocationDate)
  }

  def toTimeStampInfoProto(timestampInfo: TimestampInfo): node_api.TimestampInfo = {
    node_api
      .TimestampInfo()
      .withBlockTimestamp(timestampInfo.atalaBlockTimestamp.toEpochMilli)
      .withBlockSequenceNumber(timestampInfo.atalaBlockSequenceNumber)
      .withOperationSequenceNumber(timestampInfo.operationSequenceNumber)
  }

  def toDIDDataProto(didData: models.DIDData): node_models.DIDData = {
    node_models
      .DIDData()
      .withId(didData.didSuffix.suffix)
      .withPublicKeys(
        didData.keys.map(key => toProtoPublicKey(key.keyId, toECKeyData(key.key), toProtoKeyUsage(key.keyUsage)))
      )
  }

  def toProtoPublicKey(
      id: String,
      ecKeyData: node_models.ECKeyData,
      keyUsage: node_models.KeyUsage
  ): node_models.PublicKey = {
    node_models
      .PublicKey()
      .withId(id)
      .withEcKeyData(ecKeyData)
      .withUsage(keyUsage)
  }

  def toECKeyData(key: PublicKey): node_models.ECKeyData = {
    val point = ECKeys.getECPoint(key)
    node_models
      .ECKeyData()
      .withCurve(ECKeys.CURVE_NAME)
      .withX(ByteString.copyFrom(point.getAffineX.toByteArray))
      .withY(ByteString.copyFrom(point.getAffineY.toByteArray))
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
  def fromTimestampInfoProto(timestampInfoProto: node_api.TimestampInfo): TimestampInfo = {
    TimestampInfo(
      Instant.ofEpochMilli(timestampInfoProto.blockTimestamp),
      timestampInfoProto.blockSequenceNumber,
      timestampInfoProto.operationSequenceNumber
    )
  }

  def fromProtoKey(protoKey: node_models.PublicKey): Option[PublicKey] = {
    for {
      maybeX <- protoKey.keyData.ecKeyData
      maybeY <- protoKey.keyData.ecKeyData
    } yield ECKeys.toPublicKey(maybeX.x.toByteArray, maybeY.y.toByteArray)
  }
}
