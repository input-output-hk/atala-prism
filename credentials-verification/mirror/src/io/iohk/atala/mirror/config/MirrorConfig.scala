package io.iohk.atala.mirror.config

import com.typesafe.config.Config
import io.iohk.atala.mirror.MirrorApp
import io.iohk.atala.prism.repositories.TransactorFactory

object MirrorConfig {
  def mirrorConfig(globalConfig: Config): MirrorApp.Config = {
    val port = globalConfig.getInt("grpc.port")

    MirrorApp.Config(port = port)
  }

  def transactorConfig(globalConfig: Config): TransactorFactory.Config = {
    val config = globalConfig.getConfig("db")

    val url = config.getString("url")
    val username = config.getString("username")
    val password = config.getString("password")
    TransactorFactory.Config(
      jdbcUrl = url,
      username = username,
      password = password
    )
  }
}
