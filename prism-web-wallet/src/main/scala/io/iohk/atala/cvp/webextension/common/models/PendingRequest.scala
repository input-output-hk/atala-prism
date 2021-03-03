package io.iohk.atala.cvp.webextension.common.models

import io.iohk.atala.prism.credentials.CredentialBatchId
import io.iohk.atala.prism.crypto.SHA256Digest

/**
  * Whenever a user needs to review a request, an instance of this trait is added to a queue
  */
sealed trait PendingRequest

object PendingRequest {

  final case class IssueCredential(credentialData: CredentialSubject) extends PendingRequest
  final case class RevokeCredential(
      signedCredentialStringRepresentation: String,
      batchId: CredentialBatchId,
      batchOperationHash: SHA256Digest
  ) extends PendingRequest

  final case class WithId(id: Int, request: PendingRequest)
}
