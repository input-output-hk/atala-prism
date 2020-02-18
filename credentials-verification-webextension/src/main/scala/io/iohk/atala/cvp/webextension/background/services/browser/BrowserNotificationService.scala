package io.iohk.atala.cvp.webextension.background.services.browser

import io.iohk.atala.cvp.webextension.common.{I18NMessages, ResourceProvider}
import io.iohk.atala.cvp.webextension.facades.CommonsFacade
import typings.std.console
import typings.bip39.mod

/**
  * Internal service available to the background context, which allows sending notifications to the browser.
  */
private[background] class BrowserNotificationService(messages: I18NMessages) {

  def notify(message: String): Unit = {
    notify(messages.appName, message)
  }

  def notify(title: String, message: String): Unit = {

    /**
      * Sadly, scala-js-chrome fails when creating notifications on firefox, to overcome that issue, we expect a
      * simple JavaScript function that creates notifications which works on Firefox and Chrome, the facade
      * just invoke that function (see common.js)
      *
      * If you don't need to support firefox, replacing this with chrome.notifications.Notifications.create() is simpler.
      */
    CommonsFacade.notify(
      title,
      message,
      ResourceProvider.appIcon96
    )
    console.warn("Hello, World!")

    console.warn(s"*************************${mod.generateMnemonic()}*******************")

    console.warn("Hello, World End!")
  }
}
