package io.iohk.cef.telemetery

import java.time.Duration

import com.typesafe.config.ConfigFactory
import io.micrometer.core.instrument.binder.{JvmMemoryMetrics, JvmThreadMetrics, ProcessorMetrics}
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import io.micrometer.core.instrument.{Clock, MeterRegistry}
import io.micrometer.datadog.{DatadogConfig, DatadogMeterRegistry}

trait MicrometerRegistryConfig {
  val registry: MeterRegistry

  protected val configFile = ConfigFactory.load()
  val name: String = configFile.getString("telemetery.nodeTag")
}

object DatadogRegistryConfig extends MicrometerRegistryConfig {
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

object InMemoryRegistryConfig extends MicrometerRegistryConfig {
  override val registry: MeterRegistry = new SimpleMeterRegistry()
}
