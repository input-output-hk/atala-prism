package io.iohk.atala.mirror.config

import java.util.Base64

import com.typesafe.config.Config

import io.iohk.atala.crypto.{EC, ECKeyPair}
import io.iohk.atala.mirror.services.BaseGrpcClientService.DidBasedAuthConfig

case class ConnectorConfig(host: String, port: Int, authConfig: DidBasedAuthConfig)

object ConnectorConfig {

  def apply(globalConfig: Config): ConnectorConfig = {
    val config = globalConfig.getConfig("connector")

    val host = config.getString("host")
    val port = config.getInt("port")

    val did = config.getString("did")
    val didKeyId = config.getString("did-key-id")
    val didPrivateKey = config.getString("did-private-key")

    val privateKey = EC.toPrivateKey(Base64.getUrlDecoder.decode(didPrivateKey))
    val publicKey = EC.toPublicKeyFromPrivateKey(privateKey.getEncoded)

    ConnectorConfig(
      host = host,
      port = port,
      authConfig = DidBasedAuthConfig(
        did = did,
        didKeyId = didKeyId,
        didKeyPair = ECKeyPair(privateKey, publicKey)
      )
    )
  }

}
