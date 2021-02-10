package io.iohk.atala.cvp.webextension.popup

import io.iohk.atala.cvp.webextension.background.BackgroundAPI
import io.iohk.atala.cvp.webextension.popup.models.View
import io.iohk.atala.cvp.webextension.popup.models.View.{DisplayMnemonic, Recover}
import slinky.core.StatelessComponent
import slinky.core.annotations.react
import slinky.core.facade.ReactElement
import slinky.web.html._

@react class InitialWalletView extends StatelessComponent {

  case class Props(backgroundAPI: BackgroundAPI, switchToView: (View) => Unit)

  override def render(): ReactElement = {
    div(className := "container", className := "spaceBetween")(
      div(
        className := "title_container",
        id := "title_container"
      )(
        div(className := "div_logo", id := "logoPrism", img(src := "/assets/images/prism-logo.svg")),
        div(
          className := "mainTitle",
          h2(className := "h3_title", id := "h3_title", "Welcome to your"),
          h1(
            className := "h1_title",
            id := "h1_title",
            "Atala PRISM Browser Wallet"
          ),
          p(
            className := "description",
            id := "description",
            "Register a new wallet or recover an existing wallet"
          )
        )
      ),
      div(
        className := "div__field_group",
        div(
          className := "img_cover",
          img(className := "div_img", id := "div_img", src := "/assets/images/img-wallet-register.svg")
        ),
        div(
          className := "btn_register",
          id := "registrationScreenButton",
          "Register",
          onClick := { () =>
            props.switchToView(DisplayMnemonic)
          }
        ),
        div(
          className := "btn_recover",
          id := "recoveryScreenButton",
          "Recover",
          onClick := { () =>
            props.switchToView(Recover)
          }
        )
      )
    )
  }
}
