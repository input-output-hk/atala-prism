package io.iohk.atala.cvp.webextension.testing

import scala.scalajs.js

object FakeCommonsFacade extends js.Object {
  def notify(title: String, message: String, iconUrl: String): Unit = {}

  def setBadgeText(text: String, callback: js.Function0[_]): Unit = {
    callback()
  }

  def setBadgeBackgroundColor(color: String, callback: js.Function0[_]): Unit = {
    callback()
  }

  def setPopup(popup: String, callback: js.Function0[_]): Unit = {
    callback()
  }
}
