package io.iohk.atala.cvp.webextension.background.services.node

import java.time.Instant

import io.iohk.atala.prism.credentials
import io.iohk.atala.prism.crypto.{EC, ECPublicKey}
import io.iohk.atala.prism.protos.node_models

import scala.annotation.nowarn

object NodeUtils {
  def fromProtoKey(protoKey: node_models.PublicKey): Option[ECPublicKey] = {
    for {
      maybeX <- protoKey.keyData.ecKeyData
      maybeY <- protoKey.keyData.ecKeyData
    } yield EC.toPublicKey(maybeX.x.toByteArray(), maybeY.y.toByteArray())
  }

  def fromTimestampInfoProto(timestampInfoProto: node_models.TimestampInfo): credentials.TimestampInfo = {
    credentials.TimestampInfo(
      instantFromTimestampInfoProto(timestampInfoProto),
      timestampInfoProto.blockSequenceNumber,
      timestampInfoProto.operationSequenceNumber
    )
  }

  @nowarn("msg=value blockTimestampDeprecated in class TimestampInfo is deprecated")
  private def instantFromTimestampInfoProto(in: node_models.TimestampInfo): Instant =
    in.blockTimestamp.fold(Instant.ofEpochMilli(in.blockTimestampDeprecated))(timestamp =>
      Instant.ofEpochSecond(timestamp.seconds, timestamp.nanos.toLong)
    )
}
