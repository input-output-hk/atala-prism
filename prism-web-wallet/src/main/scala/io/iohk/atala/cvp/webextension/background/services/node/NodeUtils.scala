package io.iohk.atala.cvp.webextension.background.services.node

import io.iohk.atala.cvp.webextension.util.ByteOps

import java.time.Instant
import io.iohk.atala.prism.protos.node_models
import typings.inputOutputHkPrismSdk.mod.io.iohk.atala.prism.kotlin.credentials.TimestampInfo
import typings.inputOutputHkPrismSdk.mod.io.iohk.atala.prism.kotlin.crypto.EC
import typings.inputOutputHkPrismSdk.mod.io.iohk.atala.prism.kotlin.crypto.keys.ECPublicKey
import typings.inputOutputHkPrismSdk.mod.io.iohk.atala.prism.kotlin.extras.toLong

object NodeUtils {
  def fromProtoKey(protoKey: node_models.PublicKey): Option[ECPublicKey] = {
    for {
      maybeX <- protoKey.keyData.ecKeyData
      maybeY <- protoKey.keyData.ecKeyData
    } yield EC.toPublicKeyFromBigIntegerCoordinates(
      ByteOps.convertBytesToHex(maybeX.x.toByteArray()),
      ByteOps.convertBytesToHex(maybeY.y.toByteArray())
    )
  }

  def fromTimestampInfoProto(timestampInfoProto: node_models.TimestampInfo): TimestampInfo = {
    new TimestampInfo(
      toLong(instantFromTimestampInfoProto(timestampInfoProto).toEpochMilli.toDouble),
      timestampInfoProto.blockSequenceNumber,
      timestampInfoProto.operationSequenceNumber
    )
  }

  private def instantFromTimestampInfoProto(in: node_models.TimestampInfo): Instant = {
    val timestamp = in.blockTimestamp.getOrElse(throw new RuntimeException("Missing timestamp"))
    Instant.ofEpochSecond(timestamp.seconds, timestamp.nanos.toLong)
  }
}
