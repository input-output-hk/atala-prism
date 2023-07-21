package io.iohk.atala.prism.models

import derevo.derive
import tofu.logging.derivation.loggable

import java.util.UUID

@derive(loggable)
case class ParticipantId(uuid: UUID) extends AnyVal with UUIDValue

object ParticipantId extends UUIDValue.Builder[ParticipantId]
