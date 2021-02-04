package io.iohk.atala.cvp.webextension.popup.components

import slinky.core._
import slinky.core.annotations.react
import slinky.web.html._

@react object ErrorMessage {
  case class Props(mayBeError: Option[String] = None)

  val component: FunctionalComponent[Props] = FunctionalComponent[Props] { props =>
    div(className := "errorContainer")(props.mayBeError.map { error =>
      label(className := "_label_update")(
        error,
        img(className := "errorImg", src := "/assets/images/error.svg")
      )
    })
  }
}
