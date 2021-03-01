package io.iohk.atala.cvp.webextension.background.services.node

import java.time.Instant

import io.iohk.atala.prism.credentials
import io.iohk.atala.prism.crypto.{EC, ECPublicKey}
import io.iohk.atala.prism.protos.node_models

object NodeUtils {
  def fromProtoKey(protoKey: node_models.PublicKey): Option[ECPublicKey] = {
    for {
      maybeX <- protoKey.keyData.ecKeyData
      maybeY <- protoKey.keyData.ecKeyData
    } yield EC.toPublicKey(maybeX.x.toByteArray(), maybeY.y.toByteArray())
  }

  def fromTimestampInfoProto(timestampInfoProto: node_models.TimestampInfo): credentials.TimestampInfo = {
    credentials.TimestampInfo(
      Instant.ofEpochMilli(timestampInfoProto.blockTimestamp),
      timestampInfoProto.blockSequenceNumber,
      timestampInfoProto.operationSequenceNumber
    )
  }
}
