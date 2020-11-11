package io.iohk.atala.prism.management.console.models

import java.util.UUID

final case class ParticipantId(uuid: UUID) extends AnyVal
object ParticipantId {
  def random(): ParticipantId = {
    new ParticipantId(UUID.randomUUID())
  }
}
final case class ParticipantLogo(bytes: Vector[Byte]) extends AnyVal
final case class ParticipantInfo(
    id: ParticipantId,
    name: String,
    did: String,
    logo: Option[ParticipantLogo]
)
