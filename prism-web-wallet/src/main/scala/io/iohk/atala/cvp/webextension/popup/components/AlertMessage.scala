package io.iohk.atala.cvp.webextension.popup.components

import io.iohk.atala.cvp.webextension.popup.models.Message
import io.iohk.atala.cvp.webextension.popup.models.Message.{FailMessage, SuccessMessage}
import slinky.core._
import slinky.core.annotations.react
import slinky.web.html._

@react object AlertMessage {
  case class Props(message: Message)

  val component: FunctionalComponent[Props] = FunctionalComponent[Props] { props =>
    div(className := "alertContainer")(props.message match {
      case SuccessMessage(message) => {
        label(className := "_label_update_success")(
          message,
          img(className := "successImg", src := "/assets/images/success.svg")
        )
      }
      case FailMessage(message) => {
        label(className := "_label_update_fail")(
          message,
          img(className := "errorImg", src := "/assets/images/error.svg")
        )
      }
    })
  }
}
