package io.iohk.cef.frontend.services

import io.iohk.cef.crypto._
import io.iohk.cef.error.ApplicationError
import io.iohk.cef.frontend.client.Response
import io.iohk.cef.frontend.models._
import io.iohk.cef.ledger.identity.{Claim, IdentityData, IdentityTransaction, Link, Unlink, _}
import io.iohk.cef.transactionservice._

import scala.concurrent.{ExecutionContext, Future}

class IdentityTransactionService(nodeTransactionService: NodeTransactionService[IdentityData, IdentityTransaction])(
    implicit ec: ExecutionContext
) {

  type IdentityTransactionConstructor = (String, SigningPublicKey, Signature) => IdentityTransaction

  def createIdentityTransaction(req: CreateIdentityTransactionRequest): Response[IdentityTransaction] = {

    val identityTransaction = req.data match {
      case data: ClaimData => Right(Claim(data, req.privateKey))
      case data: LinkData =>
        req.linkingIdentityPrivateKey
          .map(pk => Link(data, req.privateKey, pk))
          .toRight(CorrespondingPrivateKeyRequiredForLinkingIdentityError)
      case data: UnlinkData => Right(Unlink(data, req.privateKey))
      case data: EndorseData => Right(Endorse(data, req.privateKey))
      case data: RevokeEndorsementData => Right(RevokeEndorsement(data, req.privateKey))
      case data: GrantData =>
        req.linkingIdentityPrivateKey
          .map(pk => Grant(data, req.privateKey, pk))
          .toRight(CorrespondingPrivateKeyRequiredForLinkingIdentityError)
      case data: LinkCertificateData =>
        req.linkingIdentityPrivateKey
          .map(pk => LinkCertificate(data, req.privateKey, pk))
          .toRight(CorrespondingPrivateKeyRequiredForLinkingIdentityError)
    }
    Future(identityTransaction)
  }

  def submitIdentityTransaction(req: SubmitIdentityTransactionRequest): Response[Unit] = {

    val identityTransaction: Either[ApplicationError, IdentityTransaction] = req.data match {
      case data: ClaimData => Right(Claim(data, req.signature))
      case data: LinkData =>
        req.linkingIdentitySignature
          .map(sig => Link(data, req.signature, sig))
          .toRight(CorrespondingSignatureRequiredForLinkingIdentityError)
      case data: UnlinkData => Right(Unlink(data, req.signature))
      case data: EndorseData => Right(Endorse(data, req.signature))
      case data: RevokeEndorsementData => Right(RevokeEndorsement(data, req.signature))
      case data: GrantData =>
        (for {
          claimSignature <- req.claimSignature
          endorseSignature <- req.endorseSignature
        } yield
          Right[ApplicationError, IdentityTransaction](Grant(data, req.signature, claimSignature, endorseSignature)))
          .getOrElse(Left(CorrespondingSignatureRequiredForLinkingIdentityError))
      case data: LinkCertificateData =>
        req.signatureFromCertificate
          .map(sig => LinkCertificate(data, req.signature, sig))
          .toRight(CorrespondingSignatureRequiredForLinkingIdentityError)
      case _ => Left(UnsupportedDataTypeError(req.data))
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
  case object CorrespondingSignatureRequiredForLinkingIdentityError extends ApplicationError {
    override def toString: String = s"Corresponding signature key for the associated Public key is required:"
  }
  case class UnsupportedDataTypeError(data: IdentityTransactionData) extends ApplicationError {
    override def toString: Identity = s"The data type $data is not supported"
  }
}
