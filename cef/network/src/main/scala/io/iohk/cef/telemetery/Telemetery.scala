package io.iohk.cef.telemetery

import io.micrometer.core.instrument.MeterRegistry

trait Telemetery {

  val registry: MeterRegistry

  val nodeTag: String
}
