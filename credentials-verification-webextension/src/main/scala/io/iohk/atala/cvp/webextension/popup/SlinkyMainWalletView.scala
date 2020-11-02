package io.iohk.atala.cvp.webextension.popup

import io.iohk.atala.cvp.webextension.background.BackgroundAPI
import io.iohk.atala.cvp.webextension.background.wallet.SigningRequest
import io.iohk.atala.cvp.webextension.popup.models.View
import io.iohk.atala.cvp.webextension.popup.models.View.Unlock
import org.scalajs.dom.raw.DOMParser
import slinky.core.Component
import slinky.core.annotations.react
import slinky.core.facade.ReactElement
import slinky.web.html.{onClick, _}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.scalajs.js
import scala.util.{Failure, Success}
import typings.dompurify.mod.{^ => dompurify}

@react class SlinkyMainWalletView extends Component {

  private val domParser = new DOMParser()
  private val emptyDiv = "<div/>"

  case class Props(backgroundAPI: BackgroundAPI, switchToView: View => Unit)
  case class State(requests: List[SigningRequest], message: String)

  override def componentDidMount(): Unit = {
    loadRequests()
  }

  override def initialState: State = State(requests = Nil, "")

  private def renderTemplate(request: SigningRequest) = {
    val sanitisedHtml = dompurify.sanitize(request.subject.properties.getOrElse("html", emptyDiv))
    domParser
      .parseFromString(sanitisedHtml, "text/html")
      .documentElement
      .textContent
  }

  override def render(): ReactElement = {
    val cancelButton = div(className := "btn_cancel", id := "btn_cancel")("Cancel")

    if (state.requests.nonEmpty) {
      val requestsToSign = for (x <- state.requests) yield {
        div(
          div(dangerouslySetInnerHTML := js.Dynamic.literal(__html = renderTemplate(x))),
          cancelButton,
          div(
            className := "btn_sign",
            id := x.id.toString,
            "Sign",
            onClick := { () =>
              props.backgroundAPI.signRequestAndPublish(x.id)
            }
          )
        )
      }

      div(className := "status_container", id := "mainView")(
        h3(className := "h3_pending")(
          "Signature request"
        ),
        p(
          className := "description_signature",
          id := "description_signature",
          "You have been requested to sign the following credential:"
        )
      )
      ReactElement.iterableToElement(requestsToSign)

    } else {
      div(className := "no-pending-container", id := "mainView")(
        div(className := "img-no-pending")(img(src := "/assets/images/img-no-pending.png")),
        p(className := "welcome_text")(
          "There are no requests pending"
        ),
        div(className := "div__field_group")(
          div(className := "lock_button", onClick := { () => lockWallet() })(
            div(className := "img_lock")(
              img(src := "/assets/images/padlock.png")
            ),
            div(
              p(className := "txt_lock_button")("Lock your account")
            )
          )
        )
      )
    }
  }

  private def loadRequests(): Unit = {
    props.backgroundAPI.getSignatureRequests().map { req =>
      setState(_.copy(requests = req.requests))
    }
  }

  private def lockWallet(): Unit = {
    props.backgroundAPI.lockWallet().onComplete {
      case Success(_) => props.switchToView(Unlock)
      case Failure(ex) =>
        setState(state.copy(message = "Failed Locking wallet"))
        println(s"Failed Locking wallet : ${ex.getMessage}")
    }
  }
}
