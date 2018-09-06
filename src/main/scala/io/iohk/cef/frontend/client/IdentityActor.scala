package io.iohk.cef.frontend.client

import java.security.{PublicKey, SecureRandom}
import java.util.Base64

import akka.actor.Actor
import akka.util.ByteString
import io.iohk.cef.error.ApplicationError
import io.iohk.cef.ledger.identity.{Claim, IdentityBlockHeader, IdentityTransaction}
import io.iohk.cef.ledger.{BlockHeader, Transaction}
import akka.pattern.pipe
import io.iohk.cef.core.Envelope
import io.iohk.cef.crypto.low.{CryptoAlgorithm, DigitalSignature, decodePublicKey}

import scala.concurrent.{ExecutionContext, Future}

object IdentityClientActor {
  sealed trait ClientRequest
  sealed trait ClientResponse

  case object Type extends Enumeration(0) {
    type Type = Value
    val CLAIM = Value("CLAIM")
    val LINK = Value("LINK")
    val UNLINK = Value("UNLINK")
  }
  case class TransactionRequest(`type`: String, identity: String, publicKey: String, signature: String, ledgerId: Int)
      extends ClientRequest // Client Request to capture
  case class TransactionResponse(result: Either[ApplicationError, Unit]) extends ClientResponse

}

class IdentityTransactionClientActor(nodeCore: NodeCore[Set[PublicKey], IdentityBlockHeader, IdentityTransaction])
    extends Actor {
  import IdentityClientActor._
  import context.dispatcher
  def receive: Receive = {
    case request @ TransactionRequest("claim", _, _, _, _) => { processTransaction(request).pipeTo(sender()) }
  }

  private def processTransaction(transactionRequest: TransactionRequest)(
      implicit ec: ExecutionContext): Future[TransactionResponse] = {
    val key: PublicKey = decodePublicKey(Base64.getDecoder.decode(transactionRequest.publicKey.getBytes))
    val signature: DigitalSignature = new DigitalSignature(ByteString(transactionRequest.signature))
    val tx = Claim(identity = transactionRequest.identity, key = key, signature = signature)
    val envelope = Envelope(content = tx, ledgerId = transactionRequest.ledgerId, _ => true)
    nodeCore.receiveTransaction(envelope) map TransactionResponse
  }
}

class NodeCore[State, Header <: BlockHeader, Tx <: Transaction[State]] {

  def receiveTransaction(txEnvelope: Envelope[Tx]): Future[Either[ApplicationError, Unit]] =
    Future.successful(Right(()))

}

//class Frontend[State, Header <: BlockHeader, Tx <: Transaction[State]](nodeCore: NodeCore[State, Header, Tx]) {
//
//  def processTx(transaction: Tx, ledgerId: LedgerId): Unit = {
//    nodeCore.receiveTransaction(Envelope(transaction, ledgerId, _ => true))
//  }
//}
