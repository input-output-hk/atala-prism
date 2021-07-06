package io.iohk.atala.cvp.webextension.popup

import io.iohk.atala.cvp.webextension.background.BackgroundAPI
import io.iohk.atala.cvp.webextension.popup.models.View
import io.iohk.atala.cvp.webextension.popup.models.View.Main
import slinky.core.Component
import slinky.core.annotations.react
import slinky.core.facade.ReactElement
import slinky.web.html._

import scala.concurrent.ExecutionContext.Implicits.global

@react class WelcomeRegisterView extends Component {

  override def componentDidMount(): Unit = {
    getOperationInfo()
  }

  override def initialState: State = State(mayBeTransactionId = None)

  case class Props(backgroundAPI: BackgroundAPI, blockchainExplorerUrl: String, switchToView: (View) => Unit)

  case class State(mayBeTransactionId: Option[String])

  override def render(): ReactElement = {
    div(id := "welcomeRegisterScreen", className := "generalContainer")(
      div(className := "div_logo", id := "logoPrism", img(src := "/assets/images/prism-logo.svg")),
      div(
        className := "elementWrapper",
        div( /*this div helps the next bit of code stay centered on the page*/ ),
        div(className := "welcome_img_Container")(
          img(className := "Welcome_img", src := "/assets/images/Done.png"),
          p(className := "welcome_registration_text")("Your wallet has been successfully registered!"),
          div(className := "div__field_group")(
            state.mayBeTransactionId.map { id =>
              a(
                href := s"${props.blockchainExplorerUrl.format(id)}",
                target := "_blank",
                className := "link_to_blockchain"
              )(
                "See operation on the blockchain"
              )
            }
          )
        ),
        div(className := "input__container"),
        div(className := "div__field_group")(
          div(
            id := "nextButton",
            className := "btn_register",
            onClick := { () =>
              props.switchToView(Main)
            }
          )("Login")
        )
      )
    )
  }

  private def getOperationInfo(): Unit = {
    props.backgroundAPI.getOperationInfo().map { operationInfo =>
      setState(_.copy(mayBeTransactionId = Some(operationInfo.id)))
    }
  }
}
