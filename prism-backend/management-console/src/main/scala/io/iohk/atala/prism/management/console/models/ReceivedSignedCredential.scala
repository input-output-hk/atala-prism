package io.iohk.atala.prism.management.console.models

import java.time.Instant
import java.util.UUID

case class ReceivedSignedCredential(
    individualId: Contact.Id,
    encodedSignedCredential: String,
    receivedAt: Instant
)

class CredentialExternalId private (val value: String) extends AnyVal

object CredentialExternalId {
  def apply(value: String): CredentialExternalId = {
    require(value.trim.nonEmpty, "External credential id must contain at least one non-whitespace character")
    new CredentialExternalId(value)
  }

  def random(): CredentialExternalId = apply(UUID.randomUUID().toString)
}
