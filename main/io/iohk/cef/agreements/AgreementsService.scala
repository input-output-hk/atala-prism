package io.iohk.cef.agreements

import java.util.concurrent.ConcurrentHashMap

import io.iohk.cef.agreements.AgreementsMessage._
import io.iohk.cef.codecs.nio._
import io.iohk.cef.codecs.nio.auto._
import io.iohk.cef.network.discovery.NetworkDiscovery
import io.iohk.cef.network.transport.Transports
import io.iohk.cef.network.{ConversationalNetwork, MessageStream}

import scala.collection.JavaConverters._
import scala.reflect.runtime.universe._

class AgreementsService[T: NioCodec: TypeTag](networkDiscovery: NetworkDiscovery, transports: Transports) {

  private val activeProposals = new ConcurrentHashMap[String, Propose[T]]().asScala

  private val networkChannel = new ConversationalNetwork[AgreementMessage[T]](networkDiscovery, transports)

  // aliased to NodeId until the network can address Identities.
  private val userId: UserId = transports.peerConfig.nodeId

  // receive notifications of proposals and agreements
  val agreementEvents: MessageStream[AgreementMessage[T]] = networkChannel.messageStream.map {
    case p:Propose[T] =>
      activeProposals.put(p.correlationId, p)
      p
    case m:AgreementMessage[T] =>
      activeProposals.remove(m.correlationId)
      m
  }

  // Send agreement to a list of userId who you wish to agree something
  // Successful execution should guarantee that all parties have received the Proposal.
  def propose(correlationId: String, data: T, to: List[UserId]): Unit = {
    val proposal = Propose(correlationId, userId, data)
    to.foreach(recipient => networkChannel.sendMessage(recipient, proposal))
  }

  // agree to a proposal
  // return an Agreement to the proposer containing the data agreed to
  // (NB: this might be different to the data in the proposal
  def agree(correlationId: String, data: T): Unit = {
    val agreement = Agree(correlationId, userId, data)
    activeProposals.get(correlationId).foreach(proposal => networkChannel.sendMessage(proposal.proposedBy, agreement))
  }

  // turn down a proposal
  def decline(correlationId: String): Unit = {
    val decline = Decline[T](correlationId, userId)
    activeProposals.get(correlationId).foreach(proposal => networkChannel.sendMessage(proposal.proposedBy, decline))
  }
}
