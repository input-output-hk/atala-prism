package io.iohk.atala.cvp.webextension.popup

import com.alexitc.materialui.facade.materialUiCore.anon.PartialClassNameMapCircul
import com.alexitc.materialui.facade.materialUiCore.buttonButtonMod.ButtonProps
import com.alexitc.materialui.facade.materialUiCore.materialUiCoreStrings.indeterminate
import com.alexitc.materialui.facade.materialUiCore.mod.PropTypes.Color
import com.alexitc.materialui.facade.materialUiCore.{materialUiCoreStrings, components => mui}
import com.alexitc.materialui.facade.materialUiIcons.{components => muiIcons}
import io.iohk.atala.cvp.webextension.background.BackgroundAPI
import io.iohk.atala.cvp.webextension.common.models.PendingRequest
import io.iohk.atala.cvp.webextension.popup.models.View
import io.iohk.atala.cvp.webextension.popup.models.View.Unlock
import org.scalajs.dom.raw.DOMParser
import slinky.core.Component
import slinky.core.annotations.react
import slinky.core.facade.ReactElement
import slinky.web.html._
import typings.dompurify.mod.{^ => dompurify}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.scalajs.js
import scala.util.{Failure, Success}

@react class MainWalletView extends Component {

  private val domParser = new DOMParser()
  private val emptyDiv = "<div/>"

  case class Props(backgroundAPI: BackgroundAPI, switchToView: (View) => Unit)

  case class State(
      requests: List[PendingRequest.IssueCredential],
      id: Int,
      message: String,
      status: Option[Boolean],
      isLoading: Boolean
  )

  override def componentDidMount(): Unit = {
    loadRequests()
  }

  override def initialState: State = State(requests = Nil, 0, "", None, false)

  private def renderTemplate(request: PendingRequest.IssueCredential) = {
    val sanitisedHtml = dompurify.sanitize(request.credentialData.properties.getOrElse("html", emptyDiv))
    domParser
      .parseFromString(sanitisedHtml, "text/html")
      .documentElement
      .textContent
  }

  override def render(): ReactElement = {

    if (state.requests.nonEmpty) {
      val signingRequest = state.requests(state.id)

      div(id := "mainView", className := "spaceBetween")(
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
      div(className := "spaceBetween", id := "mainView")(
        div(className := "div_logo", id := "logoPrism", img(src := "/assets/images/prism-logo.svg")),
        div(
          div(
            className := "img_cover",
            img(className := "img-no-pending", src := "/assets/images/img-no-pending.png")
          ),
          p(className := "welcome_text")(
            "There are no requests pending"
          )
        ),
        lockButton()
      )
    }
  }

  private def templateElement(signingRequest: PendingRequest.IssueCredential) = {
    div(
      div(
        className := "credentialContainer",
        dangerouslySetInnerHTML := js.Dynamic.literal(__html = renderTemplate(signingRequest))
      ),
      br(),
      alertMessage(),
      if (state.isLoading) {
        signatureElement(signingRequest, "disabled")
      } else {
        signatureElement(signingRequest, "")
      }
    )
  }

  private def signatureElement(signingRequest: PendingRequest.IssueCredential, appendClass: String) = {
    div(className := "buttons_container")(
      rejectButton(signingRequest, appendClass),
      nextElement(signingRequest, appendClass),
      circularProgress()
    )
  }

  private def pagingElement(): ReactElement = {
    val count = state.requests.size
    val previous = math.max(state.id - 1, 0)
    val next = math.min(state.id + 1, count - 1)

    div(className := "div__field_group_mui")(
      mui.IconButton.onClick(_ => setState(_.copy(id = previous, status = None)))(muiIcons.ChevronLeftOutlined()),
      s"${state.id + 1} of $count",
      mui.IconButton.onClick(_ => setState(_.copy(id = next, status = None)))(muiIcons.ChevronRightOutlined())
    )
  }

  private def rejectButton(signingRequest: PendingRequest.IssueCredential, appendClass: String) =
    div(
      className := s"btn_cancel btn_cancel_width $appendClass",
      id := "btn_cancel",
      "Reject",
      onClick := { () => rejectRequest(signingRequest.id) }
    )

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

  private def signButton(signingRequest: PendingRequest.IssueCredential, appendClass: String) = {
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

  private def nextElement(signingRequest: PendingRequest.IssueCredential, appendClass: String) = {
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
    val successMessage = (message: String) =>
      div(className := "div__field_group_mui")(
        mui.Button.withProps(
          ButtonProps()
            .setColor(Color.default)
            .setVariant(materialUiCoreStrings.outlined)
            .setClassName("button_success_mui")
        )(muiIcons.CheckCircle().className("buttonIcon_success"))(message)
      )

    val failMessage = (message: String) =>
      div(className := "div__field_group_mui")(
        mui.Button.withProps(
          ButtonProps()
            .setColor(Color.default)
            .setVariant(materialUiCoreStrings.outlined)
            .setClassName("button_fail_mui")
        )(muiIcons.Cancel().className("buttonIcon_fail"))(message)
      )

    state.status
      .map {
        case true => successMessage(state.message)
        case false => failMessage(state.message)
      }
      .getOrElse(emptyElement)

  }

  private def loadRequests(): Unit = {
    props.backgroundAPI.getSignatureRequests().map { req =>
      // TODO: Fow now, this is the only supported request, add support for the other ones
      val issueCredentialRequests = req.requests.collect {
        case r: PendingRequest.IssueCredential => r
      }
      setState(_.copy(requests = issueCredentialRequests, status = None))
    }
  }

  private def nextCredential(): Unit = {
    loadRequests()
  }

  private def signRequest(requestId: Int): Unit = {
    setState(_.copy(isLoading = true))
    props.backgroundAPI.signRequestAndPublish(requestId).onComplete {
      case Success(_) =>
        setState(_.copy(status = Some(true), isLoading = false, message = "Credential successfully signed!"))
      case Failure(ex) =>
        setState(_.copy(status = Some(false), isLoading = false, message = "Credential signing failed"))
        println(s"Failed Credential signing : ${ex.getMessage}")
    }
  }

  private def rejectRequest(requestId: Int): Unit = {
    setState(_.copy(isLoading = true))
    props.backgroundAPI.rejectRequest(requestId).onComplete {
      case Success(_) =>
        setState(_.copy(status = Some(true), isLoading = false, message = "Credential successfully rejected"))
      case Failure(ex) =>
        setState(_.copy(status = Some(false), isLoading = false, message = "Credential failed to reject"))
        println(s"Failed Publishing Credential : ${ex.getMessage}")
    }
  }

  private def lockWallet(): Unit = {
    props.backgroundAPI.lockWallet().onComplete {
      case Success(_) => props.switchToView(Unlock)
      case Failure(ex) =>
        setState(_.copy(message = "Failed Locking wallet"))
        println(s"Failed Locking wallet : ${ex.getMessage}")
    }
  }
}
