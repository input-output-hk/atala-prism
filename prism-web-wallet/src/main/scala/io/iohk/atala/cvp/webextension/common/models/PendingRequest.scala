package io.iohk.atala.cvp.webextension.common.models

import typings.inputOutputHkPrismSdk.mod.io.iohk.atala.prism.kotlin.credentials.CredentialBatchId
import typings.inputOutputHkPrismSdk.mod.io.iohk.atala.prism.kotlin.crypto.SHA256Digest

import java.util.UUID

/**
  * Whenever a user needs to review a request, an instance of this trait is added to a queue
  */
sealed trait PendingRequest

object PendingRequest {

  final case class IssueCredential(credentialData: CredentialSubject) extends PendingRequest
  final case class RevokeCredential(
      signedCredentialStringRepresentation: String,
      batchId: CredentialBatchId,
      batchOperationHash: SHA256Digest,
      credentialId: UUID
  ) extends PendingRequest

  final case class WithId(id: Int, request: PendingRequest)

  final case class IssueCredentialWithId(id: Int, request: IssueCredential)

  final case class RevokeCredentialWithId(id: Int, request: RevokeCredential)

}
