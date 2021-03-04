package io.iohk.atala.cvp.webextension.popup.models

sealed trait Message
object Message {
  case class FailMessage(message: String) extends Message
  case class SuccessMessage(message: String) extends Message
}
