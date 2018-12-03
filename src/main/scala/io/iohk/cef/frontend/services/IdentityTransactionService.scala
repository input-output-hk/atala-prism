package io.iohk.cef.frontend.services

import io.iohk.cef.transactionservice._
import io.iohk.cef.crypto._
import io.iohk.cef.error.ApplicationError
import io.iohk.cef.frontend.client.Response
import io.iohk.cef.frontend.models.IdentityTransactionType
import io.iohk.cef.ledger.identity.{Claim, Link, Unlink}
import io.iohk.cef.ledger.identity.{IdentityTransaction, IdentityData}
import io.iohk.cef.ledger.identity._
import io.iohk.cef.frontend.models._

import scala.concurrent.Future
import scala.concurrent.ExecutionContext

class IdentityTransactionService(nodeTransactionService: NodeTransactionService[IdentityData, IdentityTransaction])(
    implicit ec: ExecutionContext) {

  type IdentityTransactionConstructor = (String, SigningPublicKey, Signature) => IdentityTransaction

  def createIdentityTransaction(req: CreateIdentityTransactionRequest): Response[IdentityTransaction] = {

    val signature = IdentityTransaction.sign(req.identity, req.`type`, req.publicKey, req.privateKey)

    val identityTransaction = createIdentityBasedOnType[IdentityTransaction](req.`type`) {
      Right(Claim(req.identity, req.publicKey, signature))
    } {
      req.linkingIdentityPrivateKey
        .map { privateKey =>
          IdentityTransaction.sign(req.identity, req.`type`, req.publicKey, privateKey)
        }
        .map { signature2 =>
          Link(req.identity, req.publicKey, signature, signature2)
        }
        .toRight(CorrespondingPrivateKeyRequiredForLinkingIdentityError)

    } {
      Right(Unlink(req.identity, req.publicKey, signature))
    }
    Future(identityTransaction)
  }

  private def createIdentityBasedOnType[R](`type`: IdentityTransactionType)(claim: => Either[ApplicationError, R])(
      link: => Either[ApplicationError, R])(unlink: => Either[ApplicationError, R]): Either[ApplicationError, R] = {
    `type` match {
      case IdentityTransactionType.Claim => claim
      case IdentityTransactionType.Link => link
      case IdentityTransactionType.Unlink => unlink
      case IdentityTransactionType.Endorse => ???
      case IdentityTransactionType.Revoke => ???

    }
  }

  def submitIdentityTransaction(req: SubmitIdentityTransactionRequest): Response[Unit] = {

    val identityTransaction = createIdentityBasedOnType[IdentityTransaction](req.`type`) {
      Right(Claim(req.identity, req.publicKey, req.signature))
    } {
      req.linkingIdentitySignature
        .map { signature =>
          Link(req.identity, req.publicKey, req.signature, signature)
        }
        .toRight(LinkingIdentitySignatureRequiredError(req.identity, req.publicKey))
    } {
      Right(Unlink(req.identity, req.publicKey, req.signature))
    }

    identityTransaction match {
      case Right(tx) => {
        val envelope =
          Envelope(content = tx, containerId = req.ledgerId, Not(Everyone))
        nodeTransactionService.receiveTransaction(envelope)
      }
      case Left(error) => Future(Left(error))

    }

  }
  case object CorrespondingPrivateKeyRequiredForLinkingIdentityError extends ApplicationError {
    override def toString: String = s"Corresponding private key for the associated Public key is required:"
  }
}
