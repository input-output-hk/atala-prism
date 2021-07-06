package io.iohk.atala.cvp.webextension.popup.components

import slinky.core._
import slinky.core.annotations.react
import slinky.web.html._

@react object ErrorMessage {
  case class Props(error: String)

  val component: FunctionalComponent[Props] = FunctionalComponent[Props] { props =>
    div(className := "errorContainer")(
      label(className := "_label_update")(
        props.error,
        img(className := "errorImg", src := "/assets/images/error.svg")
      )
    )
  }
}
