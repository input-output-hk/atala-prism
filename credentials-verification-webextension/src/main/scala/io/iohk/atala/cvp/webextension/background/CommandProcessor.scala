package io.iohk.atala.cvp.webextension.background

import io.circe.generic.auto._
import io.iohk.atala.cvp.webextension.background.models.Command.{
  KeyList,
  SignatureResult,
  SigningRequests,
  WalletStatusResult
}
import io.iohk.atala.cvp.webextension.background.models.{Command, Event}
import io.iohk.atala.cvp.webextension.background.services.browser.{BrowserActionService, BrowserNotificationService}
import io.iohk.atala.cvp.webextension.background.wallet.WalletManager

import scala.concurrent.{ExecutionContext, Future}

/**
  * Any command supported by the BackgroundAPI is handled here.
  */
private[background] class CommandProcessor(
    browserNotificationService: BrowserNotificationService,
    browserActionService: BrowserActionService,
    walletManager: WalletManager
)(implicit ec: ExecutionContext) {

  def process(command: Command): Future[CommandResponse[_]] = command match {
    case Command.SendBrowserNotification(title, message) =>
      browserNotificationService.notify(title, message)
      Future.successful(CommandResponse(Event.BrowserNotificationSent(): Event))
    case Command.ListKeys =>
      Future.successful(CommandResponse[KeyList] {
        KeyList(walletManager.listKeys().toList)
      })
    case Command.SignRequestWithKey(requestId, keyName) =>
      Future.successful(CommandResponse {
        walletManager.signWith(requestId, keyName)
      })
    case Command.RequestSignature(message) =>
      walletManager.requestSignature(message).map(SignatureResult.apply).map(CommandResponse.apply)
    case Command.GetSigningRequests =>
      Future.successful(CommandResponse {
        SigningRequests(walletManager.getSigningRequests().toList)
      })
    case Command.CreateKey(keyName) =>
      walletManager.createKey(keyName).map(_ => CommandResponse())
    case Command.GetWalletStatus =>
      walletManager.getStatus().map(WalletStatusResult.apply).map(CommandResponse.apply)
    case Command.CreateWallet(password, mnemonic, role, organisationName, logo) =>
      walletManager.createWallet(password, mnemonic, role, organisationName, logo).map(CommandResponse.apply)
    case Command.UnlockWallet(password) =>
      for {
        _ <- walletManager.unlock(password)
        _ <- browserActionService.setPopup("popup.html")
      } yield CommandResponse(())
    case Command.LockWallet() =>
      walletManager.lock()
      browserActionService.setPopup("popup-closed.html").map(CommandResponse(_))
  }
}
