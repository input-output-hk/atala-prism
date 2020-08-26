package io.iohk.atala.cvp.webextension.popup.models

import enumeratum._

sealed trait View extends EnumEntry

object View extends Enum[View] {
  val values = findValues
  case object Register extends View
  case object Recover extends View
  case object Main extends View
  case object Default extends View // Initial View
  case object Welcome extends View
  case object Unlock extends View
}
