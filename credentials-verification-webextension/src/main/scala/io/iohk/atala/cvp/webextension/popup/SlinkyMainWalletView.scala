package io.iohk.atala.cvp.webextension.popup

import io.iohk.atala.cvp.webextension.background.BackgroundAPI
import io.iohk.atala.cvp.webextension.background.wallet.SigningRequest
import io.iohk.atala.cvp.webextension.popup.models.View
import io.iohk.atala.cvp.webextension.popup.models.View.Unlock
import org.scalajs.dom.raw.DOMParser
import slinky.core.Component
import slinky.core.annotations.react
import slinky.core.facade.ReactElement
import slinky.web.html._
import typings.dompurify.mod.{^ => dompurify}
import typings.materialUiCore.anon.PartialClassNameMapCircul
import typings.materialUiCore.buttonButtonMod.ButtonProps
import typings.materialUiCore.materialUiCoreStrings.indeterminate
import typings.materialUiCore.mod.PropTypes.Color
import typings.materialUiCore.{materialUiCoreStrings, components => mui}
import typings.materialUiIcons.{components => muiIcons}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.scalajs.js
import scala.util.{Failure, Success}

@react class SlinkyMainWalletView extends Component {

  private val domParser = new DOMParser()
  private val emptyDiv = "<div/>"

  case class Props(backgroundAPI: BackgroundAPI, switchToView: View => Unit)
  case class State(
      requests: List[SigningRequest],
      id: Int,
      message: String,
      status: Option[Boolean] = None,
      isLoading: Boolean = false
  )

  override def componentDidMount(): Unit = {
    loadRequests()
  }

  override def initialState: State = State(requests = Nil, 0, "")

  private def renderTemplate(request: SigningRequest) = {
    val sanitisedHtml = dompurify.sanitize(request.subject.properties.getOrElse("html", emptyDiv))
    domParser
      .parseFromString(sanitisedHtml, "text/html")
      .documentElement
      .textContent
  }

  override def render(): ReactElement = {

    if (state.requests.nonEmpty) {
      val signingRequest = state.requests(state.id)

      div(id := "mainView", className := "status_container")(
        h3(className := "h3_pending")(
          "Signature request"
        ),
        p(
          className := "description_signature",
          id := "description_signature",
          "You have been requested to sign the following credential:"
        ),
        templateElement(signingRequest),
        pagingElement(),
        lockButton()
      )
    } else {
      div(className := "no-pending-container", id := "mainView")(
        div(className := "img-no-pending")(img(src := "/assets/images/img-no-pending.png")),
        p(className := "welcome_text")(
          "There are no requests pending"
        ),
        lockButton()
      )
    }
  }

  private def templateElement(signingRequest: SigningRequest) = {
    div(
      div(dangerouslySetInnerHTML := js.Dynamic.literal(__html = renderTemplate(signingRequest))),
      br(),
      alertMessage(),
      if (state.isLoading) {
        signatureElement(signingRequest, "disabled")
      } else {
        signatureElement(signingRequest, "")
      }
    )
  }

  private def signatureElement(signingRequest: SigningRequest, appendClass: String) = {
    div(className := "buttons_container")(
      rejectButton(appendClass),
      nextElement(signingRequest, appendClass),
      circularProgress()
    )
  }

  private def pagingElement(): ReactElement = {
    val count = state.requests.size
    val previous = math.max(state.id - 1, 0)
    val next = math.min(state.id + 1, count - 1)

    div(className := "div__field_group_mui")(
      mui.IconButton.onClick(_ => setState(_.copy(id = previous)))(muiIcons.ChevronLeftOutlined()),
      s"${state.id + 1} of $count",
      mui.IconButton.onClick(_ => setState(_.copy(id = next)))(muiIcons.ChevronRightOutlined())
    )
  }

  private def rejectButton(appendClass: String) =
    div(className := s"btn_cancel btn_cancel_width $appendClass", id := "btn_cancel", "Reject")

  private def lockButton() =
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

  private def signButton(signingRequest: SigningRequest, appendClass: String) = {
    div(
      className := s"btn_sign btn_sign_width $appendClass",
      id := signingRequest.id.toString,
      "Sign",
      onClick := { () =>
        signRequest(signingRequest.id)
      }
    )
  }

  def nextButton() =
    div(
      className := s"btn_sign btn_sign_width",
      id := "next",
      "Next",
      onClick := { () =>
        nextCredential()
      }
    )

  private def nextElement(signingRequest: SigningRequest, appendClass: String) = {
    state.status match {
      case Some(true) => nextButton()
      case Some(false) => signButton(signingRequest, appendClass)
      case None => signButton(signingRequest, appendClass)
    }
  }

  private def circularProgress() = {
    if (state.isLoading) {
      div(
        mui.CircularProgress
          .variant(indeterminate)
          .size(26)
          .classes(PartialClassNameMapCircul().setRoot("progress_bar"))
      )
    } else {
      div()
    }

  }

  private def alertMessage() = {
    val emptyElement = div()()
    val successMessage: ReactElement =
      div(className := "div__field_group_mui")(
        mui.Button.withProps(
          ButtonProps()
            .setColor(Color.default)
            .setVariant(materialUiCoreStrings.outlined)
            .setClassName("button_success_mui")
        )(muiIcons.CheckCircle().className("buttonIcon_success"))("Credential successfully signed!")
      )

    val failMessage: ReactElement =
      div(className := "div__field_group_mui")(
        mui.Button.withProps(
          ButtonProps()
            .setColor(Color.default)
            .setVariant(materialUiCoreStrings.outlined)
            .setClassName("button_fail_mui")
        )(muiIcons.Cancel().className("buttonIcon_fail"))("Credential signing failed")
      )

    state.status
      .map {
        case true => successMessage
        case false => failMessage
      }
      .getOrElse(emptyElement)

  }

  private def loadRequests(): Unit = {
    props.backgroundAPI.getSignatureRequests().map { req =>
      setState(_.copy(requests = req.requests))
    }
  }

  private def nextCredential(): Unit = {
    setState(state.copy(status = None))
    loadRequests()
  }

  private def signRequest(requestId: Int): Unit = {
    setState(state.copy(isLoading = true))
    props.backgroundAPI.signRequestAndPublish(requestId).onComplete {
      case Success(_) => setState(state.copy(status = Some(true), isLoading = false))
      case Failure(ex) =>
        setState(state.copy(status = Some(false), isLoading = false))
        println(s"Failed Publishing Credential : ${ex.getMessage}")
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
