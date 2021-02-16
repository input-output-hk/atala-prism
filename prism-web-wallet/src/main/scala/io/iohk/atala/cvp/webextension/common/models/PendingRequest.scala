package io.iohk.atala.cvp.webextension.common.models

/**
  * Whenever a user needs to review a request, an instance of this trait is added to a queue
  */
sealed trait PendingRequest

object PendingRequest {

  final case class IssueCredential(id: Int, origin: String, sessionId: String, subject: CredentialSubject)
      extends PendingRequest

  final case class RevokeCredential() extends PendingRequest
}
