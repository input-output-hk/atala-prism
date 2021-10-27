package io.iohk.atala.prism.util

import com.google.protobuf.ByteString
import io.iohk.atala.prism.crypto.keys.ECPublicKey
import io.iohk.atala.prism.crypto.ECConfig.{INSTANCE => ECConfig}
import io.iohk.atala.prism.protos.node_models

object KeyUtils {

  def createNodePublicKey(
      keyId: String,
      publicKey: ECPublicKey
  ): node_models.PublicKey = {
    val point = publicKey.getCurvePoint
    val x = ByteString.copyFrom(point.getX.bytes())
    val y = ByteString.copyFrom(point.getY.bytes())
    node_models.PublicKey(
      id = keyId,
      usage = node_models.KeyUsage.AUTHENTICATION_KEY,
      keyData = node_models.PublicKey.KeyData.EcKeyData(
        node_models.ECKeyData(curve = ECConfig.getCURVE_NAME, x = x, y = y)
      )
    )
  }

}
