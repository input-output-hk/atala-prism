package io.iohk.cef.agreements

import io.iohk.cef.network.MessageStream

trait AgreementsService[T] {
  // Send agreement to a list of userId who you wish to agree something
  // Successful execution should guarantee that all parties have received the Proposal.
  def propose(correlationId: String, data: T, to: List[UserId]): Unit

  // agree to a proposal
  // return an Agreement to the proposer containing the data agreed to
  // (NB: this might be different to the data in the proposal
  def agree(correlationId: String, data: T): Unit

  // turn down a proposal
  def decline(correlationId: String): Unit

  // receive notifications of proposals and agreements
  val agreementEvents: MessageStream[AgreementMessage[T]]
}
