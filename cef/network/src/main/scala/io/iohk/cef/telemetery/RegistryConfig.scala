package io.iohk.cef.telemetery

import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import io.micrometer.datadog.DatadogConfig

object RegistryConfig {
  val config = new DatadogConfig {
    override def get(key: String): String = null
  }

  val registry = new SimpleMeterRegistry()
}
