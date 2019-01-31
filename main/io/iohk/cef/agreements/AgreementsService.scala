package io.iohk.cef.agreements

import io.iohk.network.MessageStream
import java.util.UUID

trait AgreementsService[T] {
  // Send agreement to a list of userId who you wish to agree something
  // Successful execution should guarantee that all parties have received the Proposal.
  def propose(correlationId: UUID, data: T, to: Set[UserId]): Unit

  // agree to a proposal
  // return an Agreement to the proposer containing the data agreed to
  // (NB: this might be different to the data in the proposal
  def agree(correlationId: UUID, data: T): Unit

  // turn down a proposal
  def decline(correlationId: UUID): Unit

  // receive notifications of proposals and agreements
  val agreementEvents: MessageStream[AgreementMessage[T]]
}
