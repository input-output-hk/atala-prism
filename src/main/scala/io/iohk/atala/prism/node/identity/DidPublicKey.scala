package io.iohk.atala.prism.node.identity

import io.iohk.atala.prism.protos.node_models.PublicKey
import io.iohk.atala.prism.node.crypto.CryptoUtils.{SecpPublicKey, SecpPublicKeyOps}
import io.iohk.atala.prism.node.models.{KeyUsage => KeyUsageModel}

case class DidPublicKey(id: String, usage: KeyUsageModel, publicKey: SecpPublicKey) {
  def toProto: PublicKey =
    PublicKey(
      id = id,
      usage = usage.toProto,
      keyData = PublicKey.KeyData.CompressedEcKeyData(publicKey.toProto)
    )
}
