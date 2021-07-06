package io.iohk.atala.cvp.webextension.popup.utils

import org.scalajs.dom.html.Input

private[popup] object ValidationUtils {
  def checkPasswordErrors(passwordInput: Input, password2Input: Input): Option[String] = {
    if (passwordInput.value.isEmpty) {
      Some("Password cannot be empty")
    } else if (passwordInput.value != password2Input.value) {
      Some("Password verification does not match")
    } else {
      None
    }
  }

}
