package io.iohk.node.client

import com.google.protobuf.ByteString
import io.iohk.atala.crypto.{ECConfig, ECPublicKey}
import io.iohk.prism.protos.node_models

object ProtoUtils {
  def protoECKeyFromPublicKey(key: ECPublicKey): node_models.ECKeyData = {
    val point = key.getCurvePoint

    node_models.ECKeyData(
      curve = ECConfig.CURVE_NAME,
      x = ByteString.copyFrom(point.x.toByteArray),
      y = ByteString.copyFrom(point.y.toByteArray)
    )
  }

}
