package io.iohk.atala.cvp.webextension.popup

import io.iohk.atala.cvp.webextension.background.BackgroundAPI
import io.iohk.atala.cvp.webextension.popup.models.View
import io.iohk.atala.cvp.webextension.popup.models.View.Main
import slinky.core.{Component, StatelessComponent}
import slinky.core.annotations.react
import slinky.core.facade.ReactElement
import slinky.web.html._
import scala.concurrent.ExecutionContext.Implicits.global

@react class SlinkyWelcomeRegisterView extends Component {

  override def componentDidMount(): Unit = {
    getTransactionInfo()
  }
  override def initialState: State = State(mayBeTransactionId = None)

  case class Props(backgroundAPI: BackgroundAPI, blockchainExplorerUrl: String, switchToView: View => Unit)
  case class State(mayBeTransactionId: Option[String])

  override def render: ReactElement = {
    div(id := "welcomeRegisterScreen")(
      div(className := "welcome_img")(
        img(src := "/assets/images/Done.png")
      ),
      p(className := "welcome_registration_text")("Your wallet has been successfully registered!"),
      div(className := "input__container"),
      div(className := "div__field_group")(
        state.mayBeTransactionId.map { id =>
          a(href := s"${props.blockchainExplorerUrl.format(id)}", target := "_blank", className := "_label")(
            "See operation on the blockchain"
          )
        }
      ),
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
  }

  private def getTransactionInfo(): Unit = {
    props.backgroundAPI.getTransactionInfo().map { transactionInfo =>
      setState(_.copy(mayBeTransactionId = Some(transactionInfo.id)))
    }
  }
}
