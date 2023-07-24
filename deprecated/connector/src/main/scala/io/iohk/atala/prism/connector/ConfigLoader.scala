package io.iohk.atala.prism.connector

import com.typesafe.config.Config

class ConfigLoader {

  def nodeClientConfig(config: Config): NodeClientConfig = {
    val host = config.getString("host")
    val port = config.getInt("port")
    NodeClientConfig(host, port)
  }
}

case class NodeClientConfig(host: String, port: Int)
