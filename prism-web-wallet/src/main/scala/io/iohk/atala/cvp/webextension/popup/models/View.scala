package io.iohk.atala.cvp.webextension.popup.models

import enumeratum._
import io.iohk.atala.cvp.webextension.common.Mnemonic

sealed trait View extends EnumEntry

object View extends Enum[View] {
  val values = findValues

  case object DisplayMnemonic extends View

  case class VerifyMnemonic(data: Data) extends View

  case class OrganizationDetails(data: Data) extends View

  case object Register extends View

  case object Recover extends View

  case object Main extends View

  case object Default extends View // Initial View
  case object Welcome extends View

  case object Unlock extends View

}

case class Data(mnemonic: Mnemonic, password: String)
