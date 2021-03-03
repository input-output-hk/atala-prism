package io.iohk.atala.cvp.webextension.background

import io.circe.generic.auto._
import io.iohk.atala.cvp.webextension.background.models.Command.{
  GotRequestsRequiringManualApproval,
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
        walletManager.enqueueRequestApproval(origin, sessionId, request).map(CommandResponse.apply)
      case Command.GetRequestsRequiringManualApproval =>
        Future.successful {
          val requests = walletManager.getRequestsRequiringManualApproval().toList
          CommandResponse(GotRequestsRequiringManualApproval(requests))
        }
      case Command.SignConnectorRequest(sessionId, request) =>
        walletManager
          .signConnectorRequest(origin, sessionId, request)
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
        walletManager.approvePendingRequest(requestId).map(_ => CommandResponse(()))
      case Command.RejectPendingRequest(requestId) =>
        walletManager.rejectRequest(requestId).map(_ => CommandResponse(()))
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
