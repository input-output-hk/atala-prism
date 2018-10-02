package io.iohk.cef.main.builder.base
import com.typesafe.config.ConfigFactory

trait ConfigReaderBuilder {
  val config: com.typesafe.config.Config = ConfigFactory.load()
}
