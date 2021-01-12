package io.iohk.atala.prism.vault

import java.time.Instant
import java.util.UUID

import io.iohk.atala.prism.crypto.SHA256Digest
import io.iohk.atala.prism.identity.DID

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
    final case class Id(value: UUID) extends AnyVal
    final case class ExternalId(value: UUID) extends AnyVal
  }
}
