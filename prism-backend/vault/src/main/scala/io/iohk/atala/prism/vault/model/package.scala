package io.iohk.atala.prism.vault

import java.time.Instant
import java.util.UUID
import io.iohk.atala.prism.kotlin.crypto.SHA256Digest
import io.iohk.atala.prism.kotlin.identity.DID
import io.iohk.atala.prism.models.UUIDValue

package object model {
  final case class CreatePayload(
      externalId: Payload.ExternalId,
      hash: SHA256Digest,
      did: DID,
      content: Vector[Byte]
  )

  final case class Payload(
      id: Payload.Id,
      externalId: Payload.ExternalId,
      hash: SHA256Digest,
      did: DID,
      content: Vector[Byte],
      createdAt: Instant
  )

  object Payload {
    final case class Id(uuid: UUID) extends AnyVal with UUIDValue
    object Id extends UUIDValue.Builder[Id]

    final case class ExternalId(uuid: UUID) extends AnyVal with UUIDValue
    object ExternalId extends UUIDValue.Builder[ExternalId]
  }
}
