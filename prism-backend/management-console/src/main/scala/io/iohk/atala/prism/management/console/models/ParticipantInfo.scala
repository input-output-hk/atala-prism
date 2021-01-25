package io.iohk.atala.prism.management.console.models

import java.util.UUID
import io.iohk.atala.prism.identity.DID
import io.iohk.atala.prism.models.UUIDValue

final case class ParticipantId(uuid: UUID) extends AnyVal with UUIDValue
object ParticipantId extends UUIDValue.Builder[ParticipantId]

final case class ParticipantLogo(bytes: Vector[Byte]) extends AnyVal

final case class ParticipantInfo(
    id: ParticipantId,
    name: String,
    did: DID,
    logo: Option[ParticipantLogo]
)
