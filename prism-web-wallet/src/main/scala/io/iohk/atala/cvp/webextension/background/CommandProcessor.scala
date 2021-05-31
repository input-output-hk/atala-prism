package io.iohk.atala.cvp.webextension.background

import cats.syntax.functor._
import io.circe.generic.auto._
import io.iohk.atala.cvp.webextension.background.models.Command.{
  GotCredentialRequestsRequiringManualApproval,
  GotRequestsRequiringManualApproval,
  GotRevocationRequestsRequiringManualApproval,
  SignedConnectorResponse,
  TransactionInfo,
  WalletStatusResult
}
import io.iohk.atala.cvp.webextension.background.models.{Command, Event}
import io.iohk.atala.cvp.webextension.background.services.browser.BrowserNotificationService
import io.iohk.atala.cvp.webextension.background.wallet.WalletManager
import io.iohk.atala.cvp.webextension.circe._

import scala.concurrent.{ExecutionContext, Future}

/**
  * Any command supported by the BackgroundAPI is handled here.
  */
private[background] class CommandProcessor(
    browserNotificationService: BrowserNotificationService,
    walletManager: WalletManager
)(implicit ec: ExecutionContext) {

  def process(command: Command)(implicit origin: String): Future[CommandResponse[_]] =
    command match {
      case Command.SendBrowserNotification(title, message) =>
        browserNotificationService.notify(title, message)
        Future.successful(CommandResponse(Event.BrowserNotificationSent(): Event))
      case Command.EnqueueRequestApproval(sessionId, request) =>
        walletManager
          .enqueueRequestApproval(origin, sessionId, request)
          .map(Command.ApprovalRequestResult)
          .map(CommandResponse.apply)
      case Command.GetRequestsRequiringManualApproval =>
        Future.successful {
          val requests = walletManager.getRequestsRequiringManualApproval().toList
          CommandResponse(GotRequestsRequiringManualApproval(requests))
        }
      case Command.GetCredentialRequestsRequiringManualApproval =>
        Future.successful {
          val requests = walletManager.getCredentialIssuanceRequestsRequiringManualApproval().toList
          CommandResponse(GotCredentialRequestsRequiringManualApproval(requests))
        }
      case Command.GetRevocationRequestsRequiringManualApproval =>
        Future.successful {
          val requests = walletManager.getRevocationRequestsRequiringManualApproval().toList
          CommandResponse(GotRevocationRequestsRequiringManualApproval(requests))
        }
      case Command.SignConnectorRequest(sessionId, request, nonce) =>
        walletManager
          .signConnectorRequest(origin, sessionId, request, nonce)
          .map(SignedConnectorResponse.apply)
          .map(CommandResponse.apply)
      case Command.VerifySignedCredential(sessionId, signedCredentialStringRepresentation, encodedMerkleProof) =>
        walletManager
          .verifySignedCredential(origin, sessionId, signedCredentialStringRepresentation, encodedMerkleProof)
          .map(Command.VerifySignedCredentialResponse.apply)
          .map(CommandResponse.apply)
      case Command.GetWalletStatus =>
        walletManager.getStatus().map(WalletStatusResult.apply).map(CommandResponse.apply)
      case Command.GetUserSession =>
        walletManager.getLoggedInUserSession(origin).map(CommandResponse.apply)
      case Command.ApprovePendingRequest(requestId) =>
        walletManager.approvePendingRequest(requestId).as(CommandResponse(()))
      case Command.RejectPendingRequest(requestId) =>
        walletManager.rejectRequest(requestId).as(CommandResponse(()))
      case Command.ApproveAllCredentialRequests =>
        walletManager.approveAllCredentialRequests().map(CommandResponse(_))
      case Command.RejectAllCredentialRequests =>
        walletManager.rejectAllCredentialRequests().map(CommandResponse(_))
      case Command.CreateWallet(password, mnemonic, role, organisationName, logo) =>
        walletManager.createWallet(password, mnemonic, role, organisationName, logo).map(CommandResponse.apply)
      case Command.RecoverWallet(password, mnemonic) =>
        walletManager.recoverWallet(password, mnemonic).map(CommandResponse.apply)
      case Command.UnlockWallet(password) =>
        walletManager.unlock(password).map(CommandResponse.apply)
      case Command.LockWallet() =>
        walletManager.lock().map(CommandResponse(_))
      case Command.GetTransactionInfo =>
        walletManager.getTransactionId().map(TransactionInfo.apply).map(CommandResponse.apply)
    }
}
