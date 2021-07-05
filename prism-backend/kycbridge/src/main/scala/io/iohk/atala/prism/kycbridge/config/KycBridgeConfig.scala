package io.iohk.atala.prism.kycbridge.config

import com.typesafe.config.Config
import io.iohk.atala.prism.task.lease.system.ProcessingTaskLeaseConfig
import io.iohk.atala.prism.utils.GrpcUtils.GrpcConfig

case class AcuantConfig(
    assureIdUrl: String,
    acasUrl: String,
    faceIdUrl: String,
    username: String,
    password: String,
    subscriptionId: String,
    identityMind: IdentityMindConfig
)

case class KycBridgeConfig(
    grpcConfig: GrpcConfig,
    acuantConfig: AcuantConfig,
    taskLeaseConfig: ProcessingTaskLeaseConfig
)

case class IdentityMindConfig(
    url: String,
    username: String,
    password: String,
    profile: String
)

object KycBridgeConfig {
  def apply(config: Config): KycBridgeConfig = {
    val acuantConfig = config.getConfig("acuant")

    KycBridgeConfig(
      grpcConfig = GrpcConfig(config),
      acuantConfig = AcuantConfig(
        assureIdUrl = acuantConfig.getString("assureIdUrl"),
        acasUrl = acuantConfig.getString("acasUrl"),
        faceIdUrl = acuantConfig.getString("faceId"),
        username = acuantConfig.getString("username"),
        password = acuantConfig.getString("password"),
        subscriptionId = acuantConfig.getString("subscriptionId"),
        identityMind = getIdentityMindConfig(acuantConfig)
      ),
      taskLeaseConfig = ProcessingTaskLeaseConfig(config.getConfig("taskLeaseSystem"))
    )
  }

  private[config] def getIdentityMindConfig(config: Config): IdentityMindConfig = {
    val identityMind = config.getConfig("identityMind")

    IdentityMindConfig(
      url = identityMind.getString("url"),
      username = identityMind.getString("username"),
      password = identityMind.getString("password"),
      profile = identityMind.getString("profile")
    )
  }
}
