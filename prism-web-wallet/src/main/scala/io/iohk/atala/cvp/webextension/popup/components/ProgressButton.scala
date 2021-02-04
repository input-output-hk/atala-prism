package io.iohk.atala.cvp.webextension.popup.components

import com.alexitc.materialui.facade.materialUiCore.anon.PartialClassNameMapCircul
import com.alexitc.materialui.facade.materialUiCore.materialUiCoreStrings.{indeterminate}
import com.alexitc.materialui.facade.materialUiCore.{components => mui}
import slinky.core._
import slinky.core.annotations.react
import slinky.web.html._

@react object ProgressButton {

  case class Props(buttonLabel: String, isButtonEnable: Boolean, isLoading: Boolean, onClickCallFunc: Boolean => Unit)

  val component: FunctionalComponent[Props] = FunctionalComponent[Props] { props =>
    val enabledClass =
      if (props.isButtonEnable && !props.isLoading) "progressBtn" else "progressBtn disabled"

    div(className := "div__field_group")(
      div(
        id := "progressButton",
        className := enabledClass,
        onClick := { () =>
          props.onClickCallFunc(!props.isLoading)
        }
      )(props.buttonLabel),
      if (props.isLoading) {
        mui.CircularProgress
          .variant(indeterminate)
          .size(26)
          .classes(PartialClassNameMapCircul().setRoot("progress_bar"))
      } else {
        div()
      }
    )
  }
}
