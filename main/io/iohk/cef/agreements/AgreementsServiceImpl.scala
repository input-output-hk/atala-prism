package io.iohk.cef.agreements

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

import io.iohk.cef.agreements.AgreementsMessage._
import io.iohk.cef.network.{ConversationalNetwork, MessageStream}

import scala.collection.JavaConverters._

class AgreementsServiceImpl[T](network: ConversationalNetwork[AgreementMessage[T]]) extends AgreementsService[T] {

  private val proposalsReceived = new ConcurrentHashMap[UUID, Propose[T]]().asScala

  // aliased to NodeId until the network can address Identities.
  private val userId: UserId = network.peerConfig.nodeId

  // receive notifications of proposals and agreements
  override val agreementEvents: MessageStream[AgreementMessage[T]] = network.messageStream.map {
    case p: Propose[T] =>
      proposalsReceived.put(p.correlationId, p)
      p
    case e => e
  }

  // Send agreement to a list of userId who you wish to agree something
  // Successful execution should guarantee that all parties have received the Proposal.
  override def propose(correlationId: UUID, data: T, to: Set[UserId]): Unit = {
    val proposal = Propose(correlationId, userId, data)
    to.foreach(recipient => network.sendMessage(recipient, proposal))
  }

  // agree to a proposal
  // return an Agreement to the proposer containing the data agreed to
  // (NB: this might be different to the data in the proposal
  override def agree(correlationId: UUID, data: T): Unit = {
    require(proposalsReceived.contains(correlationId), s"Unknown correlationId '${correlationId}'.")
    val proposal = proposalReceived(correlationId)
    try {
      network.sendMessage(proposal.proposedBy, Agree(correlationId, userId, data))
    } finally {
      proposalsReceived.remove(correlationId)
    }
  }

  // turn down a proposal
  override def decline(correlationId: UUID): Unit = {
    require(proposalsReceived.contains(correlationId), s"Unknown correlationId '${correlationId}'.")
    val proposal = proposalReceived(correlationId)
    try {
      network.sendMessage(proposal.proposedBy, Decline[T](correlationId, userId))
    } finally {
      proposalsReceived.remove(correlationId)
    }
  }

  private def proposalReceived(correlationId: UUID): Propose[T] =
    proposalsReceived.getOrElse(
      correlationId,
      throw new IllegalArgumentException(s"Unknown correlationId '$correlationId'.")
    )
}
