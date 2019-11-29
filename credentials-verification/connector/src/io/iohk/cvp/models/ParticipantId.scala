package io.iohk.cvp.models

import java.util.UUID

case class ParticipantId(uuid: UUID) extends AnyVal

object ParticipantId {
  def random(): ParticipantId = {
    new ParticipantId(UUID.randomUUID())
  }
}
