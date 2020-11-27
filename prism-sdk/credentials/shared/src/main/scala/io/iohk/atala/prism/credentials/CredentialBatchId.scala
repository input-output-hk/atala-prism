package io.iohk.atala.prism.credentials

import java.util.UUID

import io.iohk.atala.prism.crypto.SHA256Digest

class CredentialBatchId private (val id: String) extends AnyVal

object CredentialBatchId {
  private val CREDENTIAL_BATCH_ID_RE = "^[0-9a-f]{64}$".r

  def fromString(id: String): Option[CredentialBatchId] = {
    if (CREDENTIAL_BATCH_ID_RE.pattern.matcher(id).matches()) Some(new CredentialBatchId(id))
    else None
  }

  def fromDigest(digest: SHA256Digest): Option[CredentialBatchId] = fromString(digest.hexValue)

  def random(): CredentialBatchId =
    new CredentialBatchId(SHA256Digest.compute(UUID.randomUUID().toString.getBytes()).hexValue)
}
