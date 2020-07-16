package io.iohk.node.client

import java.security.{PublicKey => JPublicKey}

import com.google.protobuf.ByteString
import io.iohk.cvp.crypto.ECKeys
import io.iohk.prism.protos.node_models

object ProtoUtils {
  def protoECKeyFromPublicKey(key: JPublicKey): node_models.ECKeyData = {
    val point = ECKeys.getECPoint(key)

    node_models.ECKeyData(
      curve = ECKeys.CURVE_NAME,
      x = ByteString.copyFrom(point.getAffineX.toByteArray),
      y = ByteString.copyFrom(point.getAffineY.toByteArray)
    )
  }

}
