package io.iohk.atala.cvp.webextension.popup.components

import com.alexitc.materialui.facade.materialUiCore.anon.PartialClassNameMapLinear
import com.alexitc.materialui.facade.materialUiCore.materialUiCoreStrings.indeterminate
import com.alexitc.materialui.facade.materialUiCore.{materialUiCoreStrings, components => mui}
import slinky.core._
import slinky.core.annotations.react
import slinky.core.facade.Hooks
import slinky.web.html.{div, onClick, _}

@react object ConfirmOperationModal {
  private val disableButtonCssClass = "disabled"

  case class Props(
      count: Int,
      message: String,
      confirm: () => Unit,
      cancel: () => Unit
  )

  val component: FunctionalComponent[Props] = FunctionalComponent[Props] { props =>
    val (isLoading, setIsLoading) = Hooks.useState(false)
    val disableButtonCSSClass = if (isLoading) disableButtonCssClass else ""

    mui
      .Modal(true)(
        div(className := "modal-dialog")(
          div(
            div(className := "signImageContainer", img(src := "/assets/images/signIcon.svg")),
            p(
              className := "smallTitle",
              id := "description1",
              s"${props.message}"
            )
          ),
          div(
            p(
              className := "descriptionModal",
              id := "description1",
              s"${props.count} credential(s)"
            ),
            p(
              className := "descriptionModal2",
              id := "description2",
              "By confirming you accept to have reviewed all the credential(s) data"
            )
          ),
          div(
            className := "flex",
            div(
              className := s"btn_cancel btn_modal_cancel_width $disableButtonCSSClass",
              id := "btn_cancel",
              "Cancel",
              onClick := { () => props.cancel() }
            ),
            div(
              className := s"btn_confirm btn_modal_confirm_width $disableButtonCSSClass",
              id := "btn_confirm",
              "Confirm",
              onClick := { () =>
                {
                  setIsLoading(true)
                  props.confirm()
                }
              }
            ),
            linearProgressBar(isLoading)
          )
        )
      )
  }

  private def linearProgressBar(isLoading: Boolean) = {
    if (isLoading) {
      div(
        mui.LinearProgress
          .variant(indeterminate)
          .color(materialUiCoreStrings.secondary)
          .classes(PartialClassNameMapLinear().setRoot("progress_linear_bar"))
      )
    } else {
      div()
    }
  }

}
