package io.iohk.atala.mirror.config

import com.typesafe.config.Config
import io.iohk.atala.prism.utils.GrpcUtils.GrpcConfig

case class HttpConfig(payIdPort: Int, payIdHostAddress: String)

case class MirrorConfig(grpcConfig: GrpcConfig, httpConfig: HttpConfig, trisaConfig: TrisaConfig)

case class TrisaConfig(
    serverCertificateLocation: String,
    serverCertificatePrivateKeyLocation: String,
    serverTrustChainLocation: String
)

object MirrorConfig {

  def apply(globalConfig: Config): MirrorConfig = {
    val grpcConfig = GrpcConfig(globalConfig)

    val http = globalConfig.getConfig("http")
    val httpConfig = HttpConfig(
      payIdPort = http.getInt("pay-id-port"),
      payIdHostAddress = http.getString("pay-id-host-address")
    )

    val trisa = globalConfig.getConfig("trisa")
    val trisaConfig = TrisaConfig(
      serverCertificateLocation = trisa.getString("serverCertificateLocation"),
      serverCertificatePrivateKeyLocation = trisa.getString("serverCertificatePrivateKeyLocation"),
      serverTrustChainLocation = trisa.getString("serverTrustChainLocation")
    )

    MirrorConfig(grpcConfig, httpConfig, trisaConfig)
  }

}
