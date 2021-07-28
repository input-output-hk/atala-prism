package io.iohk.atala.prism.logging

import derevo.derive
import tofu.logging.derivation.loggable

import java.util.UUID

@derive(loggable)
final case class TraceId(traceId: UUID) extends AnyVal

object TraceId {
  def generateYOLO: TraceId = TraceId(UUID.randomUUID())
}
