package io.iohk.atala.identity

import java.util.Base64

import com.google.protobuf.ByteString
import io.iohk.atala.crypto.{ECConfig, ECPublicKey, SHA256Digest}
import io.iohk.prism.protos.node_models

object DID {
  def createUnpublishedDID(masterKey: ECPublicKey): String = {
    val createDidOp = node_models.CreateDIDOperation(
      didData = Some(
        node_models.DIDData(
          publicKeys = Seq(
            node_models.PublicKey(
              id = s"master0",
              usage = node_models.KeyUsage.MASTER_KEY,
              keyData = node_models.PublicKey.KeyData.EcKeyData(
                publicKeyToProto(masterKey)
              )
            )
          )
        )
      )
    )

    val atalaOp = node_models.AtalaOperation(operation = node_models.AtalaOperation.Operation.CreateDid(createDidOp))
    val operationBytes = atalaOp.toByteArray
    val operationHash = SHA256Digest.compute(operationBytes)
    val didSuffix = operationHash.hexValue
    val encodedOperation = Base64.getUrlEncoder.encodeToString(operationBytes)
    s"did:prism:$didSuffix:$encodedOperation"
  }

  private def publicKeyToProto(key: ECPublicKey): node_models.ECKeyData = {
    val point = key.getCurvePoint
    node_models.ECKeyData(
      curve = ECConfig.CURVE_NAME,
      x = ByteString.copyFrom(point.x.toByteArray),
      y = ByteString.copyFrom(point.y.toByteArray)
    )
  }
}
