package io.iohk.cef.telemetery

import io.micrometer.core.instrument.Clock
import io.micrometer.datadog.{DatadogConfig, DatadogMeterRegistry}

object Datadog {
  val config = new DatadogConfig {
    override def get(key: String): String = null
  }

  val registry = new DatadogMeterRegistry(config, Clock.SYSTEM)

}
