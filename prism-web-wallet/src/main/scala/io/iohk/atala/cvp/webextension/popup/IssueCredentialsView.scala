package io.iohk.atala.cvp.webextension.popup

import com.alexitc.materialui.facade.materialUiCore.{materialUiCoreStrings, components => mui}
import io.iohk.atala.cvp.webextension.background.BackgroundAPI
import io.iohk.atala.cvp.webextension.common.models.PendingRequest.IssueCredentialWithId
import io.iohk.atala.cvp.webextension.popup.components.{
  AlertMessage,
  ConfirmOperationModal,
  LockButton,
  PaginationButtons
}
import io.iohk.atala.cvp.webextension.popup.models.Message.{FailMessage, SuccessMessage}
import io.iohk.atala.cvp.webextension.popup.models.View.{Default, Main}
import io.iohk.atala.cvp.webextension.popup.models.{Message, View}
import org.scalajs.dom.raw.DOMParser
import slinky.core.FunctionalComponent
import slinky.core.annotations.react
import slinky.core.facade.{Hooks, ReactElement, SetStateHookCallback}
import slinky.web.html.{div, _}
import typings.dompurify.mod.{^ => dompurify}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.scalajs.js
import scala.util.{Failure, Success}

@react object IssueCredentialsView {

  private val domParser = new DOMParser()
  private val emptyDiv = "<div/>"
  private val disableButtonCssClass = "disabled" //refer popup.css
  private val signMessageLabel = "You are about sign:"
  private val rejectMessageLabel = "You are about reject:"
  type Func = () => Unit

  case class Props(backgroundAPI: BackgroundAPI, switchToView: (View) => Unit)

  case class State(
      requests: List[IssueCredentialWithId],
      id: Int,
      message: Option[Message],
      status: Option[Boolean],
      isLoading: Boolean,
      showConfirmDialog: Boolean,
      cssClass: String
  )

  private def initialState: State = State(requests = Nil, 0, None, None, false, false, "")

  private def renderTemplate(html: String) = {
    val sanitisedHtml = dompurify.sanitize(html)
    domParser
      .parseFromString(sanitisedHtml, "text/html")
      .documentElement
      .textContent
  }

  val component = FunctionalComponent[Props] { props =>
    val (state, setState) = Hooks.useState[State](initialState)
    val (messageLabel, setMessageLabel) = Hooks.useState[String]("")
    val (operation, setOperation) = Hooks.useState[Func](() => ())

    Hooks.useEffect(() => loadRequests(props, setState), "")

    div(id := "mainView", className := "spaceBetween")(
      div(
        className := "containerPadding",
        div(
          className := "div_logo",
          id := "logoPrism",
          img(className := "logoImage", src := "/assets/images/prism-logo.svg")
        ),
        mui
          .Button(
            div(className := "backArrow", onClick := { () => props.switchToView(Default) })(
              img(className := "leftArrow", src := "/assets/images/arrow-l.svg"),
              p("Back")
            )
          )
          .className("muiButton")
          .size(materialUiCoreStrings.small),
        h3(className := "h3_pending")(
          "Signature request"
        ),
        p(
          className := "description_signature",
          id := "description_signature",
          "You have been requested to sign the following operations:"
        ),
        if (state.requests.nonEmpty) {
          val signingRequest = state.requests(state.id)
          div(
            templateElement(props, setState, setMessageLabel, setOperation, state, signingRequest),
            PaginationButtons(state.requests.size, state.id, pageNumber => setPageNumber(pageNumber, setState)),
            LockButton(props.backgroundAPI, message => onError(message, setState), props.switchToView),
            displayOperationDialog(setState, state, messageLabel, operation)
          )
        } else {
          div()
        }
      )
    )

  }

  private def displayOperationDialog(
      setState: SetStateHookCallback[State],
      state: State,
      messageLabel: String,
      operation: Func
  ): ReactElement = {
    if (state.showConfirmDialog) {
      div()(
        ConfirmOperationModal(
          state.requests.size,
          messageLabel,
          () => operation.apply(),
          () => cancel(setState)
        )
      )
    } else {
      div()
    }
  }

  private def templateElement(
      props: Props,
      setState: SetStateHookCallback[State],
      setMessageLabel: SetStateHookCallback[String],
      setOperation: SetStateHookCallback[Func],
      state: State,
      signingRequest: IssueCredentialWithId
  ) = {

    // TODO: This needs to be validated before accepting the request, so that signingRequest.html is available
    val html = signingRequest.request.credentialData.properties.getOrElse("html", emptyDiv)
    div(
      div(
        className := "credentialContainer",
        dangerouslySetInnerHTML := js.Dynamic.literal(__html = renderTemplate(html))
      ),
      br(),
      div(
        className := "btnContainer",
        p(className := s"rejectBtn ${state.cssClass}")("Reject this credential"),
        onClick := { () => rejectRequest(props, setState, signingRequest.id) }
      ),
      state.message.map(msg => AlertMessage(msg)),
      if (state.isLoading) {
        signatureElement(props, state, setState, setMessageLabel, setOperation, signingRequest)
      } else {
        signatureElement(props, state, setState, setMessageLabel, setOperation, signingRequest)
      }
    )
  }

  private def onError(message: Message, setState: SetStateHookCallback[State]): Unit = {
    setState(_.copy(message = Some(message)))
  }

  private def setPageNumber(pageNumber: Int, setState: SetStateHookCallback[State]): Unit = {
    setState(_.copy(id = pageNumber, status = None))
  }

  private def signatureElement(
      props: Props,
      state: State,
      setState: SetStateHookCallback[State],
      setMessageLabel: SetStateHookCallback[String],
      setOperation: SetStateHookCallback[Func],
      signingRequest: IssueCredentialWithId
  ) = {
    div(className := "buttons_container")(
      rejectButton(props, setState, setMessageLabel, setOperation, state.cssClass),
      nextElement(props, setState, setMessageLabel, setOperation, state, signingRequest)
    )
  }

  private def rejectButton(
      props: Props,
      setState: SetStateHookCallback[State],
      setMessageLabel: SetStateHookCallback[String],
      setOperation: SetStateHookCallback[Func],
      cssClass: String
  ) =
    div(
      className := s"btn_cancel btn_cancel_width $cssClass",
      id := "btn_cancel",
      "Reject All",
      onClick := { () =>
        setState(_.copy(showConfirmDialog = true))
        setMessageLabel(rejectMessageLabel)
        setOperation(() => rejectAllPendingRequests(props, setState))
      }
    )

  private def signButton(
      props: Props,
      setState: SetStateHookCallback[State],
      setMessageLabel: SetStateHookCallback[String],
      setOperation: SetStateHookCallback[Func],
      signingRequest: IssueCredentialWithId
  ) = {
    val label = "Sign All"
    div(
      className := s"btn_sign btn_sign_width",
      id := signingRequest.id.toString,
      label,
      onClick := { () =>
        setState(_.copy(showConfirmDialog = true))
        setMessageLabel(signMessageLabel)
        setOperation(() => signAllPendingRequests(props, setState))
      }
    )
  }
  private def nextButton(props: Props, state: State, setState: SetStateHookCallback[State]) = {
    val buttonLabel = if (state.cssClass == disableButtonCssClass) "View Dashboard" else "Next"
    div(
      className := s"btn_sign btn_sign_width",
      id := "next",
      buttonLabel,
      onClick := { () =>
        nextCredential(props, setState)
      }
    )
  }

  private def nextElement(
      props: Props,
      setState: SetStateHookCallback[State],
      setMessageLabel: SetStateHookCallback[String],
      setOperation: SetStateHookCallback[Func],
      state: State,
      signingRequest: IssueCredentialWithId
  ) = {
    state.status match {
      case Some(true) => nextButton(props, state, setState)
      case Some(false) | None => signButton(props, setState, setMessageLabel, setOperation, signingRequest)
    }
  }

  private def loadRequests(props: Props, setState: SetStateHookCallback[State]): Unit = {
    props.backgroundAPI.getCredentialSignatureRequests().map { req =>
      val issueCredentials = req.requests
      if (issueCredentials.nonEmpty)
        setState(_.copy(requests = issueCredentials, id = 0, status = None))
      else props.switchToView(Main)
    }
  }

  private def nextCredential(props: Props, setState: SetStateHookCallback[State]): Unit = {
    loadRequests(props, setState)
  }

  private def signAllPendingRequests(
      props: Props,
      setState: SetStateHookCallback[State]
  ): Unit = {
    setState(_.copy(isLoading = true, showConfirmDialog = true))
    props.backgroundAPI.approveAllCredentialRequests() onComplete {
      case Success(_) =>
        setState(
          _.copy(
            status = Some(true),
            isLoading = false,
            showConfirmDialog = false,
            cssClass = disableButtonCssClass,
            message = Some(SuccessMessage("Credentials successfully signed."))
          )
        )
      case Failure(ex) =>
        setState(
          _.copy(
            status = Some(false),
            isLoading = false,
            showConfirmDialog = false,
            message = Some(FailMessage("Credential failed to sign."))
          )
        )
        println(s"Failed Publishing Credentials : ${ex.getMessage}")
    }
  }

  private def rejectAllPendingRequests(
      props: Props,
      setState: SetStateHookCallback[State]
  ): Unit = {
    setState(_.copy(isLoading = true, showConfirmDialog = true))
    props.backgroundAPI.rejectAllCredentialRequests().onComplete {
      case Success(_) =>
        setState(
          _.copy(
            status = Some(true),
            isLoading = false,
            showConfirmDialog = false,
            cssClass = disableButtonCssClass,
            message = Some(SuccessMessage("Credentials successfully rejected."))
          )
        )
      case Failure(ex) =>
        setState(
          _.copy(
            status = Some(false),
            isLoading = false,
            showConfirmDialog = false,
            message = Some(FailMessage("Credential failed to reject."))
          )
        )
        println(s"Failed Publishing Credentials : ${ex.getMessage}")
    }
  }

  private def rejectRequest(props: Props, setState: SetStateHookCallback[State], requestId: Int): Unit = {
    setState(_.copy(isLoading = true))
    props.backgroundAPI.rejectRequest(requestId).onComplete {
      case Success(_) =>
        setState(
          _.copy(
            status = Some(true),
            isLoading = false,
            showConfirmDialog = false,
            message = Some(SuccessMessage("Credential successfully rejected."))
          )
        )
      case Failure(ex) =>
        setState(
          _.copy(
            status = Some(false),
            isLoading = false,
            showConfirmDialog = false,
            message = Some(FailMessage("Credential failed to reject."))
          )
        )
        println(s"Failed Publishing Credential : ${ex.getMessage}")
    }
  }

  private def cancel(setState: SetStateHookCallback[State]): Unit = {
    setState(_.copy(showConfirmDialog = false))
    println("cancel")
  }
}
