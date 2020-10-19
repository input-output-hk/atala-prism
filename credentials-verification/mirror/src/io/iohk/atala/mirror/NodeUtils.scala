package io.iohk.atala.mirror

import java.time.Instant

import com.google.protobuf.ByteString
import io.iohk.atala.prism.credentials
import io.iohk.atala.prism.crypto.{EC, ECConfig, ECPublicKey}
import io.iohk.atala.prism.protos.node_models

object NodeUtils {
  def fromProtoKey(protoKey: node_models.PublicKey): Option[ECPublicKey] = {
    for {
      maybeX <- protoKey.keyData.ecKeyData
      maybeY <- protoKey.keyData.ecKeyData
    } yield EC.toPublicKey(maybeX.x.toByteArray, maybeY.y.toByteArray)
  }

  def toTimestampInfoProto(ecPublicKey: ECPublicKey): node_models.ECKeyData = {
    val point = ecPublicKey.getCurvePoint

    node_models.ECKeyData(
      ECConfig.CURVE_NAME,
      x = ByteString.copyFrom(point.x.toByteArray),
      y = ByteString.copyFrom(point.y.toByteArray)
    )
  }

  def fromTimestampInfoProto(timestampInfoProto: node_models.TimestampInfo): credentials.TimestampInfo = {
    credentials.TimestampInfo(
      Instant.ofEpochMilli(timestampInfoProto.blockTimestamp),
      timestampInfoProto.blockSequenceNumber,
      timestampInfoProto.operationSequenceNumber
    )
  }

  def toInfoProto(timestampInfoProto: credentials.TimestampInfo): node_models.TimestampInfo = {
    node_models.TimestampInfo(
      timestampInfoProto.atalaBlockTimestamp.toEpochMilli,
      timestampInfoProto.atalaBlockSequenceNumber,
      timestampInfoProto.operationSequenceNumber
    )
  }
}
