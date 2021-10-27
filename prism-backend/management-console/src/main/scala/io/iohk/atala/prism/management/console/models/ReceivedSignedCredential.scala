package io.iohk.atala.prism.management.console.models

import derevo.derive
import tofu.logging.derivation.loggable

import java.time.Instant
import java.util.UUID
import scala.util.Try

case class ReceivedSignedCredential(
    individualId: Contact.Id,
    encodedSignedCredential: String,
    receivedAt: Instant
)
@derive(loggable)
class CredentialExternalId private (val value: String) extends AnyVal

object CredentialExternalId {
  def apply(value: String): CredentialExternalId = {
    require(
      value.trim.nonEmpty,
      "External credential id must contain at least one non-whitespace character"
    )
    new CredentialExternalId(value)
  }

  def from(value: String): Try[CredentialExternalId] =
    Try(apply(value))

  def random(): CredentialExternalId = apply(UUID.randomUUID().toString)
}
