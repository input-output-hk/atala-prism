package io.iohk.atala.prism.config

import com.typesafe.config.Config

case class ConnectorConfig(host: String, port: Int)

object ConnectorConfig {

  def apply(globalConfig: Config): ConnectorConfig = {
    val config = globalConfig.getConfig("connector")

    val host = config.getString("host")
    val port = config.getInt("port")

    ConnectorConfig(
      host = host,
      port = port
    )
  }
}
