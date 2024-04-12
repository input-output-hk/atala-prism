package io.iohk.atala.prism.node.config

import com.typesafe.config.Config

case class NodeConfig(host: String, port: Int)

object NodeConfig {

  def apply(globalConfig: Config): NodeConfig = {
    val config = globalConfig.getConfig("node")

    val host = config.getString("host")
    val port = config.getInt("port")

    NodeConfig(
      host = host,
      port = port
    )
  }

}
