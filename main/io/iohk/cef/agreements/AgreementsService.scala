package io.iohk.cef.agreements

import java.util.concurrent.ConcurrentHashMap

import io.iohk.cef.agreements.AgreementsMessage._
import io.iohk.cef.network.{ConversationalNetwork, MessageStream}

import scala.collection.JavaConverters._

class AgreementsService[T](network: ConversationalNetwork[AgreementMessage[T]]) {

  private val proposalsReceived = new ConcurrentHashMap[String, Propose[T]]().asScala

  // aliased to NodeId until the network can address Identities.
  private val userId: UserId = network.peerConfig.nodeId

  // receive notifications of proposals and agreements
  val agreementEvents: MessageStream[AgreementMessage[T]] = network.messageStream.map {
    case p: Propose[T] =>
      proposalsReceived.put(p.correlationId, p)
      p
    case e => e
  }

  // Send agreement to a list of userId who you wish to agree something
  // Successful execution should guarantee that all parties have received the Proposal.
  def propose(correlationId: String, data: T, to: List[UserId]): Unit = {
    val proposal = Propose(correlationId, userId, data)
    to.foreach(recipient => network.sendMessage(recipient, proposal))
  }

  // agree to a proposal
  // return an Agreement to the proposer containing the data agreed to
  // (NB: this might be different to the data in the proposal
  def agree(correlationId: String, data: T): Unit = {
    val proposal = proposalReceived(correlationId)
    try {
      network.sendMessage(proposal.proposedBy, Agree(correlationId, userId, data))
    } finally {
      proposalsReceived.remove(correlationId)
    }
  }

  // turn down a proposal
  def decline(correlationId: String): Unit = {
    val proposal = proposalReceived(correlationId)
    try {
      network.sendMessage(proposal.proposedBy, Decline[T](correlationId, userId))
    } finally {
      proposalsReceived.remove(correlationId)
    }
  }

  private def proposalReceived(correlationId: String): Propose[T] =
    proposalsReceived.getOrElse(
      correlationId,
      throw new IllegalArgumentException(s"Unknown correlationId '$correlationId'.")
    )
}
