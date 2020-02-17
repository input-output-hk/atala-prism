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
import io.iohk.atala.cvp.webextension.background.services.storage.StorageService
import io.iohk.atala.cvp.webextension.background.wallet.WalletManager

import scala.concurrent.{ExecutionContext, Future}

/**
  * Any command supported by the BackgroundAPI is handled here.
  */
private[background] class CommandProcessor(
    productStorage: StorageService,
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
      walletManager.requestSignature(message).map(s => CommandResponse(SignatureResult(s)))
    case Command.GetSigningRequests =>
      Future.successful(CommandResponse {
        SigningRequests(walletManager.getSigningRequests().toList)
      })
    case Command.CreateKey(keyName) =>
      Future.successful(CommandResponse {
        walletManager.createKey(keyName)
        ()
      })
    case Command.GetWalletStatus =>
      walletManager.getStatus().map(status => CommandResponse(WalletStatusResult(status)))
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
