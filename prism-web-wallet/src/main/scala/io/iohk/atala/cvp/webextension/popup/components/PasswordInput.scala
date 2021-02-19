package io.iohk.atala.cvp.webextension.popup.components

import slinky.core._
import slinky.core.annotations.react
import slinky.core.facade.Hooks
import slinky.web.html._

@react object PasswordInput {
  case class Props(
      label: String,
      placeHolder: String,
      password: String,
      setPassword: String => Unit,
      validate: Option[String => Unit] = None
  )

  val component: FunctionalComponent[Props] = FunctionalComponent[Props] { props =>
    val (inputType, setInputType) = Hooks.useState("password")

    def toggle(): Unit = {
      if (inputType == "password") {
        setInputType("text")
      } else setInputType("password")
    }

    div(className := "div__field_group")(
      label(className := "_label")(props.label),
      div(className := "input__container")(
        div(img(className := "eye-btn", src := "/assets/images/eye-open.svg"), onClick := (e => toggle())),
        input(
          id := "password",
          className := "_input",
          `type` := inputType,
          placeholder := props.placeHolder,
          value := props.password,
          onChange := (e => props.setPassword(e.target.value)),
          onBlur := (e => {
            props.validate.map(_.apply(e.target.value))
          })
        )
      )
    )
  }

}
