package io.iohk.cef.frontend.client

import java.security.PublicKey
import java.util.Base64

import akka.actor.Actor
import akka.pattern.pipe
import akka.util.ByteString
import io.iohk.cef.core.Envelope
import io.iohk.cef.crypto.low.{DigitalSignature, decodePublicKey}
import io.iohk.cef.error.ApplicationError
import io.iohk.cef.ledger.identity._
import io.iohk.cef.ledger.{BlockHeader, Transaction}

import scala.concurrent.{ExecutionContext, Future}

object IdentityClientActor {
  sealed trait ClientRequest
  sealed trait ClientResponse


  case class TransactionRequest(`type`: String, identity: String, publicKey: String, signature: String, ledgerId: Int)
      extends ClientRequest // Client Request to capture
  case class TransactionResponse(result: Either[ApplicationError, Unit]) extends ClientResponse

}

class IdentityTransactionClientActor(nodeCore: NodeCore[Set[PublicKey], IdentityBlockHeader, IdentityTransaction])
    extends Actor {
  import IdentityClientActor._
  import context.dispatcher
  def receive: Receive = {
    case request @ TransactionRequest("claim", _, _, _, _) => {
      processTransaction(request) {
        val (key, signature) = getKeyAndSignatureFromRequest(request)
        Claim(request.identity, key, signature)
      }.pipeTo(sender())
    }
    case request @ TransactionRequest("link", _, _, _, _) => {
      processTransaction(request) {
        val (key, signature) = getKeyAndSignatureFromRequest(request)
        Link(request.identity, key, signature)
      }.pipeTo(sender())
    }
    case request @ TransactionRequest("unlink", _, _, _, _) => {
      processTransaction(request) {
        val (key, signature) = getKeyAndSignatureFromRequest(request)
        Unlink(request.identity, key, signature)
      }.pipeTo(sender())
    }

  }

  private def processTransaction(transactionRequest: TransactionRequest)(identityTransaction: IdentityTransaction)(
      implicit ec: ExecutionContext): Future[TransactionResponse] = {
    val envelope = Envelope(content = identityTransaction, ledgerId = transactionRequest.ledgerId, _ => true)
    nodeCore.receiveTransaction(envelope) map TransactionResponse
  }
  private def getKeyAndSignatureFromRequest(transactionRequest: TransactionRequest) = {
    val key: PublicKey = decodePublicKey(Base64.getDecoder.decode(transactionRequest.publicKey.getBytes))
    val signature: DigitalSignature = new DigitalSignature(ByteString(transactionRequest.signature))
    (key, signature)
  }
}

class NodeCore[State, Header <: BlockHeader, Tx <: Transaction[State]] {

  def receiveTransaction(txEnvelope: Envelope[Tx]): Future[Either[ApplicationError, Unit]] =
    Future.successful(Right(()))

}
