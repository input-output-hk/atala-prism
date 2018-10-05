package io.iohk.cef.main.builder
import com.typesafe.config.ConfigFactory

trait ConfigReaderBuilder {
  val config: com.typesafe.config.Config = ConfigFactory.load()
}
