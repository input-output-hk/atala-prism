package io.iohk.atala.prism.util

import com.google.protobuf.ByteString
import io.iohk.atala.prism.crypto.{ECConfig, ECPublicKey}
import io.iohk.atala.prism.protos.node_models

object KeyUtils {

  def createNodePublicKey(keyId: String, publicKey: ECPublicKey): node_models.PublicKey = {
    val point = publicKey.getCurvePoint
    val x = ByteString.copyFrom(point.x.toByteArray)
    val y = ByteString.copyFrom(point.y.toByteArray)
    node_models.PublicKey(
      id = keyId,
      usage = node_models.KeyUsage.AUTHENTICATION_KEY,
      keyData =
        node_models.PublicKey.KeyData.EcKeyData(node_models.ECKeyData(curve = ECConfig.CURVE_NAME, x = x, y = y))
    )
  }

}
