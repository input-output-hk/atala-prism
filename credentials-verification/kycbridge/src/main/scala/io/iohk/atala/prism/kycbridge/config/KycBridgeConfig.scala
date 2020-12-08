package io.iohk.atala.prism.kycbridge.config

import com.typesafe.config.Config

case class AssureIdConfig(url: String, username: String, password: String, subscriptionId: String)

case class KycBridgeConfig(assureIdConfig: AssureIdConfig)

object KycBridgeConfig {
  def apply(config: Config): KycBridgeConfig = {
    val assureIdConfig = config.getConfig("assureId")

    KycBridgeConfig(
      assureIdConfig = AssureIdConfig(
        url = assureIdConfig.getString("url"),
        username = assureIdConfig.getString("username"),
        password = assureIdConfig.getString("password"),
        subscriptionId = assureIdConfig.getString("subscriptionId")
      )
    )
  }
}
