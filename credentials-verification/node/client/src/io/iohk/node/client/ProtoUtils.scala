package io.iohk.node.client

import java.security.{PublicKey => JPublicKey}

import com.google.protobuf.ByteString
import io.iohk.cvp.crypto.ECKeys
import io.iohk.node.geud_node.ECKeyData

object ProtoUtils {
  def protoECKeyFromPublicKey(key: JPublicKey): ECKeyData = {
    val point = ECKeys.getECPoint(key)

    ECKeyData(
      curve = ECKeys.CURVE_NAME,
      x = ByteString.copyFrom(point.getAffineX.toByteArray),
      y = ByteString.copyFrom(point.getAffineY.toByteArray)
    )
  }

}
