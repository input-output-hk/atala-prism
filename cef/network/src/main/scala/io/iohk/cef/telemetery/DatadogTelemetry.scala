package io.iohk.cef.telemetery
import java.time.Duration

import io.micrometer.core.instrument.binder.{JvmMemoryMetrics, JvmThreadMetrics, ProcessorMetrics}
import io.micrometer.core.instrument.{Clock, MeterRegistry, Tag}
import io.micrometer.datadog.{DatadogConfig, DatadogMeterRegistry}

import collection.JavaConverters._

trait DatadogTelemetry extends Telemetery {

  override val registry: MeterRegistry = DatadogTelemetry.registry

  val tags = List(Tag.of("node", DatadogTelemetry.nodeTag)).asJava

  registry.config().commonTags(tags)
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
