package io.iohk.cvp.models

import java.util.UUID

case class ParticipantId(uuid: UUID) extends AnyVal

object ParticipantId {
  def apply(uuid: String): ParticipantId = apply(UUID.fromString(uuid))

  def random(): ParticipantId = {
    new ParticipantId(UUID.randomUUID())
  }
}
