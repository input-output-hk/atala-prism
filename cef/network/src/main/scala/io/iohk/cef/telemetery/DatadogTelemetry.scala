package io.iohk.cef.telemetery
import java.time.Duration

import io.micrometer.core.instrument.binder.jvm.{JvmMemoryMetrics, JvmThreadMetrics}
import io.micrometer.core.instrument.binder.system.ProcessorMetrics
import io.micrometer.core.instrument.{Clock, MeterRegistry}
import io.micrometer.datadog.{DatadogConfig, DatadogMeterRegistry}

trait DatadogTelemetry extends Telemetery {

  override val registry: MeterRegistry = DatadogTelemetry.registry

  override val nodeTag: String = DatadogTelemetry.nodeTag
}

object DatadogTelemetry extends MicrometerRegistryConfig {
  self =>

  val step: Duration = configFile.getDuration("telemetery.datadog.duration")

  val apiKey: String = configFile.getString("telemetery.datadog.apiKey")

  val config = new DatadogConfig() {
    override def apiKey(): String = self.apiKey

    override def step(): Duration = self.step

    override def get(key: String): String = null
  }

  override val registry = new DatadogMeterRegistry(config, Clock.SYSTEM)

  new JvmMemoryMetrics().bindTo(registry)
  new ProcessorMetrics().bindTo(registry)
  new JvmThreadMetrics().bindTo(registry)

}
