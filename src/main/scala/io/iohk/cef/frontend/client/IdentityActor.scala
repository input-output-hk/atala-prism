package io.iohk.cef.frontend.client

import java.security.interfaces.RSAPublicKey
import java.util.Base64

import akka.actor.Actor
import akka.util.ByteString
import com.sun.org.apache.xml.internal.security.transforms.implementations.TransformBase64Decode
import io.iohk.cef.error.ApplicationError
import io.iohk.cef.ledger.identity.{Claim, IdentityBlockHeader, IdentityTransaction}
import io.iohk.cef.ledger.{BlockHeader, Transaction}

import scala.concurrent.{ExecutionContext, Future}

object IdentityClientActor {
  sealed trait ClientRequest
  sealed trait ClientResponse

  case class TransactionRequest(`type`: String, identity: String, key: String, ledgerId: Int) extends ClientRequest // Client Request to capture

  sealed trait TransactionResponse extends ClientResponse
  case object IdentityClaimCreated extends TransactionResponse
  case object IdentityClaimFailed extends TransactionResponse

}

class IdentityTransactionClientActor(nodeCore: NodeCore[Set[ByteString], IdentityBlockHeader, IdentityTransaction])
    extends Actor {
  import IdentityClientActor._
  import context.dispatcher
  def receive: Receive = {
    case request @ TransactionRequest("claim", _, _, _) => { sender ! processTransaction(request) }
  }

  private def processTransaction(transactionRequest: TransactionRequest)(
      implicit ec: ExecutionContext): Future[TransactionResponse] = {

    val tx = Claim(identity = transactionRequest.identity, key = ByteString(transactionRequest.key))
    val envelope = Envelope(content = tx, ledgerId = transactionRequest.ledgerId)
    nodeCore.receiveTransaction(envelope) map {
      case Right(_) => IdentityClaimCreated
      case Left(_) => IdentityClaimFailed
    }
  }
}

case class Envelope[+D](content: D, ledgerId: Int, destinationDescriptor: Boolean = true)

class NodeCore[State, Header <: BlockHeader, Tx <: Transaction[State]] {

  def receiveTransaction(txEnvelope: Envelope[Tx]): Future[Either[ApplicationError, Unit]] =
    Future.successful(Right(()))

}
