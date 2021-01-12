package io.iohk.atala.prism.kycbridge.config

import com.typesafe.config.Config
import io.iohk.atala.prism.utils.GrpcUtils.GrpcConfig

case class AcuantConfig(
    assureIdUrl: String,
    acasUrl: String,
    username: String,
    password: String,
    subscriptionId: String
)

case class KycBridgeConfig(grpcConfig: GrpcConfig, acuantConfig: AcuantConfig)

object KycBridgeConfig {
  def apply(config: Config): KycBridgeConfig = {
    val acuantConfig = config.getConfig("acuant")

    KycBridgeConfig(
      grpcConfig = GrpcConfig(config),
      acuantConfig = AcuantConfig(
        assureIdUrl = acuantConfig.getString("assureIdUrl"),
        acasUrl = acuantConfig.getString("acasUrl"),
        username = acuantConfig.getString("username"),
        password = acuantConfig.getString("password"),
        subscriptionId = acuantConfig.getString("subscriptionId")
      )
    )
  }
}
