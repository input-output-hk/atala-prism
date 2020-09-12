package io.iohk.atala.mirror.config

import com.typesafe.config.Config

case class TransactorConfig(driver: String, username: String, password: String, jdbcUrl: String)

object TransactorConfig {

  def apply(globalConfig: Config): TransactorConfig = {
    val config = globalConfig.getConfig("db")

    val url = config.getString("url")
    val username = config.getString("username")
    val password = config.getString("password")

    TransactorConfig(
      driver = "org.postgresql.Driver", // This isn't likely to be replaced ever
      jdbcUrl = url,
      username = username,
      password = password
    )
  }

}
