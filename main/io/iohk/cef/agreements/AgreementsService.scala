package io.iohk.cef.agreements

import java.util.concurrent.ConcurrentHashMap

import io.iohk.cef.agreements.AgreementsMessage._
import io.iohk.cef.codecs.nio._
import io.iohk.cef.codecs.nio.auto._
import io.iohk.cef.network.discovery.NetworkDiscovery
import io.iohk.cef.network.transport.Transports
import io.iohk.cef.network.{ConversationalNetwork, MessageStream}
import org.slf4j.LoggerFactory

import scala.collection.JavaConverters._
import scala.reflect.runtime.universe._

class AgreementsService[T: NioCodec: TypeTag](networkDiscovery: NetworkDiscovery, transports: Transports) {

  private val log = LoggerFactory.getLogger(classOf[AgreementsService[T]])

  private val activeProposals = new ConcurrentHashMap[String, Propose[T]]().asScala

  private val networkChannel = new ConversationalNetwork[AgreementMessage[T]](networkDiscovery, transports)

  // aliased to NodeId until the network can address Identities.
  private val userId: UserId = transports.peerConfig.nodeId

  networkChannel.messageStream.foreach {
    case p:Propose[T] => activeProposals.put(p.correlationId, p); println(s"AS internal for '$userId' handles: $p")
    case a:Agree[T] => activeProposals.remove(a.correlationId); println(s"AS internal for '$userId' handles: $a")
    case d:Decline[T] => activeProposals.remove(d.correlationId); println(s"AS internal for '$userId' handles: $d")
  }

  // receive notifications of proposals and agreements
  def agreementEvents: MessageStream[AgreementMessage[T]] = networkChannel.messageStream

  // Send agreement to a list of userId who you wish to agree something
  // Successful execution should guarantee that all parties have received the Proposal.
  def propose(correlationId: String, data: T, to: List[UserId]): Unit = {
    val proposal = Propose(correlationId, userId, data)
    println(s"User '$userId' proposes '$proposal'")
    to.foreach(recipient => networkChannel.sendMessage(recipient, proposal))
  }

  // agree to a proposal
  // return an Agreement to the proposer containing the data agreed to
  // (NB: this might be different to the data in the proposal
  def agree(correlationId: String, data: T): Unit = {
    val agreement = Agree(correlationId, userId, data)
    println(s"User '$userId' agrees '$agreement'")
    activeProposals.get(correlationId).foreach(proposal => networkChannel.sendMessage(proposal.proposedBy, agreement))
  }

  // turn down a proposal
  def decline(correlationId: String): Unit = {
    val decline = Decline[T](correlationId, userId)
    println(s"User '$userId' declines '$decline'")
    activeProposals.get(correlationId).foreach(proposal => networkChannel.sendMessage(proposal.proposedBy, decline))
  }
}
