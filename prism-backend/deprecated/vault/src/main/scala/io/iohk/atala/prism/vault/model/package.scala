package io.iohk.atala.prism.vault

import java.time.Instant
import java.util.UUID
import io.iohk.atala.prism.crypto.Sha256Digest
import io.iohk.atala.prism.identity.{PrismDid => DID}
import io.iohk.atala.prism.models.UUIDValue
import tofu.logging.derivation.loggable
import derevo.derive

package object model {
  final case class CreatePayload(
      externalId: Payload.ExternalId,
      hash: Sha256Digest,
      did: DID,
      content: Vector[Byte]
  )

  final case class Payload(
      id: Payload.Id,
      externalId: Payload.ExternalId,
      hash: Sha256Digest,
      did: DID,
      content: Vector[Byte],
      createdAt: Instant
  )

  object Payload {
    @derive(loggable)
    final case class Id(uuid: UUID) extends AnyVal with UUIDValue
    object Id extends UUIDValue.Builder[Id]
    @derive(loggable)
    final case class ExternalId(uuid: UUID) extends AnyVal with UUIDValue
    object ExternalId extends UUIDValue.Builder[ExternalId]
  }
}
