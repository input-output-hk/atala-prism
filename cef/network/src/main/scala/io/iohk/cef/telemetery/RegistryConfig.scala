package io.iohk.cef.telemetery

import java.time.Duration

import com.typesafe.config.ConfigFactory
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import io.micrometer.core.instrument.{Clock, MeterRegistry}
import io.micrometer.datadog.{DatadogConfig, DatadogMeterRegistry}

trait MicrometerRegistryConfig {
  val registry: MeterRegistry
  val name: String
}

object DatadogRegistryConfig extends MicrometerRegistryConfig {
  self =>

  val configFile = ConfigFactory.load()

  override val name: String = configFile.getString("telemetery.name")

  val step: Duration = configFile.getDuration("telemetery.datadog.duration")

  val apiKey: String = configFile.getString("telemetery.datadog.apiKey")

  val config = new DatadogConfig() {
    override def apiKey(): String = self.apiKey

    override def step(): Duration = self.step

    override def get(key: String): String = null
  }

  override val registry = new DatadogMeterRegistry(config, Clock.SYSTEM)
}

object InMemoryRegistryConfig extends MicrometerRegistryConfig {

  val configFile = ConfigFactory.load()

  override val name: String = configFile.getString("telemetery.name")
  override val registry: MeterRegistry = new SimpleMeterRegistry()
}
