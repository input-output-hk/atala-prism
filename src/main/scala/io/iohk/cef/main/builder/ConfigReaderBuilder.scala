package io.iohk.cef.main.builder
import com.typesafe.config.ConfigFactory

class ConfigReaderBuilder {
  val config: com.typesafe.config.Config = ConfigFactory.load()
}
