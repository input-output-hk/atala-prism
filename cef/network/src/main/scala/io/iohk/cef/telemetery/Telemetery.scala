package io.iohk.cef.telemetery

import io.micrometer.core.instrument.MeterRegistry

trait Telemetery {

  def registry: MeterRegistry
}
