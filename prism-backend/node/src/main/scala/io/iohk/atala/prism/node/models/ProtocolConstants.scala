package io.iohk.atala.prism.node.models

import com.typesafe.config.{Config, ConfigFactory}
import util.Try

object ProtocolConstants {

  private val globalConfig: Config = ConfigFactory.load()

  private val defaultServiceLimit = 50
  private val defaultServiceEndpointCharLength = 300
  private val defaultServiceTypeCharLength = 100

  val servicesLimit: Int = Try(globalConfig.getInt("didServicesLimit")).toOption.getOrElse(defaultServiceLimit)
  val serviceEndpointCharLenLimit: Int = Try(globalConfig.getInt("didServiceEndpointCharLimit")).toOption.getOrElse(defaultServiceEndpointCharLength)
  val serviceTypeCharLimit: Int = Try(globalConfig.getInt("didServiceTypeCharLimit")).toOption.getOrElse(defaultServiceTypeCharLength)

}
