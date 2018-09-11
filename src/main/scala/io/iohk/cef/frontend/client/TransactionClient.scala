package io.iohk.cef.frontend.client

import java.security.PublicKey
import akka.actor.Actor
import akka.pattern.pipe
import akka.util.ByteString
import io.iohk.cef.core.{Envelope, NodeCore}
import io.iohk.cef.crypto.low.{DigitalSignature, decodePublicKey}
import io.iohk.cef.error.ApplicationError
import io.iohk.cef.ledger.identity._
import org.bouncycastle.util.encoders.Hex

import scala.concurrent.{ExecutionContext, Future}

object TransactionClient {
  sealed trait ClientRequest
  sealed trait ClientResponse

  case class IdentityTransactionRequest(
      `type`: String,
      identity: String,
      publicKey: String,
      signature: String,
      ledgerId: Int)
      extends ClientRequest // Client Request to capture

  case class TransactionResponse(result: Either[ApplicationError, Unit]) extends ClientResponse

}

/**
  *  A transaction Client Actor to create identity related transaction using node core.
  * @param nodeCore
  */
class IdentityTransactionClientActor(nodeCore: NodeCore[Set[PublicKey], IdentityBlockHeader, IdentityTransaction])
    extends Actor {
  import TransactionClient._
  import context.dispatcher
  def receive: Receive = {
    case request @ IdentityTransactionRequest("claim", _, _, _, _) =>
      processTransaction(request)(Claim.apply).pipeTo(sender())
    case request @ IdentityTransactionRequest("link", _, _, _, _) =>
      processTransaction(request)(Link.apply).pipeTo(sender())
    case request @ IdentityTransactionRequest("unlink", _, _, _, _) =>
      processTransaction(request)(Unlink.apply).pipeTo(sender())

  }

  private def processTransaction(request: IdentityTransactionRequest)(
      constructor: (String, PublicKey, DigitalSignature) => IdentityTransaction)(
      implicit ec: ExecutionContext): Future[TransactionResponse] = {

    val key: PublicKey = decodePublicKey(Hex.decode(request.publicKey))
    val signature: DigitalSignature = new DigitalSignature(ByteString(request.signature))
    val envelope =
      Envelope(content = constructor(request.identity, key, signature), ledgerId = request.ledgerId, _ => true)

    nodeCore.receiveTransaction(envelope) map TransactionResponse
  }

}
