package io.iohk.cef.agreements
import java.util.concurrent.Future

import io.iohk.cef.codecs.nio._
import io.iohk.cef.network.{MessageStream, NodeId, PeerConfig}

object Agreements {
  type UserId = NodeId
}
import Agreements._

/* Message types for proposing data and agreeing to that data */
sealed trait AgreementMessage[T]

case class Propose[T](correlationId: String, proposedBy: UserId, data: T) extends AgreementMessage[T]

case class Agree[T](correlationId: String, agreedBy: UserId, data: T) extends AgreementMessage[T]

case class Decline[T](correlationId: String, declinedBy: UserId) extends AgreementMessage[T]

class AgreementsService[T: NioCodec](peerConfig: PeerConfig) {
  /* Send agreement to a list of userId who you wish to agree something */
  /* Successful execution should guarantee that all parties have received the Proposal. */
  def propose(correlationId: String, data: T, to: List[UserId]): Future[Unit] = ???

  /* agree to a proposal */
  /* return an Agreement to the proposer containing the data agreed to */
  /* (NB: this might be different to the data in the proposal */
  def agree(correlationId: String, data: T): Future[Unit] = ???

  /* receive notifications of proposals and agreements */
  val agreementEvents: MessageStream[AgreementMessage[T]] = ???
}
