package io.iohk.atala.prism.models

import java.util.UUID

case class ParticipantId(uuid: UUID) extends AnyVal with UUIDValue

object ParticipantId extends UUIDValue.Builder[ParticipantId]
