package io.iohk.cef.frontend.services

import io.iohk.cef.core._
import io.iohk.cef.crypto._
import io.iohk.cef.frontend.client.Response
import io.iohk.cef.frontend.models.IdentityTransactionType
import io.iohk.cef.ledger.identity.{Claim, Link, Unlink}
import io.iohk.cef.ledger.identity.IdentityTransaction
import io.iohk.cef.frontend.models._

import scala.concurrent.Future
import scala.concurrent.ExecutionContext

class IdentityTransactionService(nodeCore: NodeCore[Set[SigningPublicKey], IdentityTransaction])(
    implicit ec: ExecutionContext) {

  type IdentityTransactionConstructor = (String, SigningPublicKey, Signature) => IdentityTransaction

  def createIdentityTransaction(req: CreateIdentityTransactionRequest): Response[IdentityTransaction] = {

    val signature = IdentityTransaction.sign(req.identity, req.`type`, req.publicKey, req.privateKey)

    val identityTransaction: IdentityTransaction =
      constructorType(req.`type`)(req.identity, req.publicKey, signature)

    Future(Right(identityTransaction))
  }

  def submitIdentityTransaction(req: SubmitIdentityTransactionRequest): Response[Unit] = {
    val identityTransaction: IdentityTransaction =
      constructorType(req.`type`)(req.identity, req.publicKey, req.signature)

    val envelope =
      Envelope(content = identityTransaction, containerId = req.ledgerId, Not(Everyone))

    nodeCore.receiveTransaction(envelope)

  }

  private def constructorType(`type`: IdentityTransactionType): IdentityTransactionConstructor = {
    `type` match {
      case IdentityTransactionType.Link => Link.apply
      case IdentityTransactionType.Claim => Claim.apply
      case IdentityTransactionType.Unlink => Unlink.apply
    }
  }

}
