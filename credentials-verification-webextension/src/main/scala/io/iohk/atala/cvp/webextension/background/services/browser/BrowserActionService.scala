package io.iohk.atala.cvp.webextension.background.services.browser

import io.iohk.atala.cvp.webextension.facades.CommonsFacade

import scala.concurrent.{Future, Promise}

class BrowserActionService {
  def setBadgeText(text: String, callback: () => Unit): Unit = CommonsFacade.setBadgeText(text, callback)
  def setBadgeText(text: String): Future[Unit] = {
    val promise = Promise[Unit]()
    CommonsFacade.setBadgeText(text, () => promise.success(()))
    promise.future
  }

  def setBadgeBackgroundColor(color: String, callback: () => Unit): Unit = {
    CommonsFacade.setBadgeBackgroundColor(color, callback)
  }

  def setBadgeBackgroundColor(color: String): Future[Unit] = {
    val promise = Promise[Unit]()
    CommonsFacade.setBadgeBackgroundColor(color, () => promise.success(()))
    promise.future
  }

  def setPopup(popup: String, callback: () => Unit): Unit = {
    CommonsFacade.setPopup(popup, callback)
  }

  def setPopup(popup: String): Future[Unit] = {
    val promise = Promise[Unit]()
    CommonsFacade.setPopup(popup, () => promise.success(()))
    promise.future
  }
}
