package io.iohk.cef.telemetery

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.simple.SimpleMeterRegistry

trait InMemoryTelemetry extends Telemetery {

  override val registry: MeterRegistry = InMemoryTelemetry.registry

  override val nodeTag: String = InMemoryTelemetry.nodeTag
}

object InMemoryTelemetry extends MicrometerRegistryConfig {
  val registry: MeterRegistry = new SimpleMeterRegistry()
}
