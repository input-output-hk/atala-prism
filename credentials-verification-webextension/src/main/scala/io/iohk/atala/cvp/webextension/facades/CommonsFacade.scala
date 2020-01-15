package io.iohk.atala.cvp.webextension.facades

import scala.scalajs.js
import scala.scalajs.js.annotation.JSGlobal

/**
  * A facade for the functions on scripts/common.js
  */
@js.native
@JSGlobal("facade")
object CommonsFacade extends js.Object {

  def notify(title: String, message: String, iconUrl: String): Unit = js.native

  def setBadgeText(text: String, callback: js.Function0[_]): Unit = js.native
  def setBadgeBackgroundColor(color: String, callback: js.Function0[_]): Unit = js.native
  def setPopup(popup: String, callback: js.Function0[_]): Unit = js.native
}
