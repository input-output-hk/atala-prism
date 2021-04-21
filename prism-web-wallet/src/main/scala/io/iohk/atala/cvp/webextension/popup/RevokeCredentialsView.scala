package io.iohk.atala.cvp.webextension.popup

import com.alexitc.materialui.facade.materialUiCore.anon.PartialClassNameMapCircul
import com.alexitc.materialui.facade.materialUiCore.materialUiCoreStrings.indeterminate
import com.alexitc.materialui.facade.materialUiCore.{materialUiCoreStrings, components => mui}
import io.iohk.atala.cvp.webextension.background.BackgroundAPI
import io.iohk.atala.cvp.webextension.common.models.PendingRequest.RevokeCredentialWithId
import io.iohk.atala.cvp.webextension.popup.components.{AlertMessage, LockButton, PaginationButtons}
import io.iohk.atala.cvp.webextension.popup.models.Message.{FailMessage, SuccessMessage}
import io.iohk.atala.cvp.webextension.popup.models.View.Main
import io.iohk.atala.cvp.webextension.popup.models.{Message, View}
import org.scalajs.dom.raw.DOMParser
import slinky.core.FunctionalComponent
import slinky.core.annotations.react
import slinky.core.facade.{Hooks, SetStateHookCallback}
import slinky.web.html._
import typings.dompurify.mod.{^ => dompurify}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.scalajs.js
import scala.util.{Failure, Success}

@react object RevokeCredentialsView {

  private val domParser = new DOMParser()

  case class Props(backgroundAPI: BackgroundAPI, switchToView: (View) => Unit)

  case class State(
      requests: List[RevokeCredentialWithId],
      id: Int,
      message: Option[Message],
      status: Option[Boolean],
      isLoading: Boolean
  )

  private def initialState: State = State(requests = Nil, 0, None, None, false)

  private def renderTemplate(html: String) = {
    val sanitisedHtml = dompurify.sanitize(html)
    domParser
      .parseFromString(sanitisedHtml, "text/html")
      .documentElement
      .textContent
  }

  val component = FunctionalComponent[Props] { props =>
    val (state, setState) = Hooks.useState[State](initialState)
    Hooks.useEffect(() => loadRequests(props, setState), "")

    div(id := "mainView", className := "spaceBetween")(
      div(
        className := "div_logo",
        id := "logoPrism",
        img(className := "logoImage", src := "/assets/images/prism-logo.svg")
      ),
      mui
        .Button(
          div(className := "backArrow", onClick := { () => props.switchToView(Main) })(
            img(className := "leftArrow", src := "/assets/images/arrow-l.svg"),
            p("Back")
          )
        )
        .className("muiButton")
        .size(materialUiCoreStrings.small),
      h3(className := "h3_pending")(
        "Revocation request"
      ),
      p(
        className := "description_signature",
        id := "description_signature",
        "You have been requested to revoke the following credentials:"
      ),
      templateView(props, setState, state),
      LockButton(props.backgroundAPI, message => onError(message, setState), props.switchToView)
    )

  }

  private def onError(message: Message, setState: SetStateHookCallback[State]): Unit = {
    setState(_.copy(message = Some(message)))
  }

  private def setPageNumber(pageNumber: Int, setState: SetStateHookCallback[State]): Unit = {
    setState(_.copy(id = pageNumber, status = None))
  }

  private def templateView(props: Props, setState: SetStateHookCallback[State], state: State) = {

    if (state.requests.nonEmpty) {
      val signingRequest = state.requests(state.id)
      div()(
        templateElement(props, setState, state, signingRequest),
        PaginationButtons(state.requests.size, state.id, pageNumber => setPageNumber(pageNumber, setState))
      )
    } else {
      div()()
    }
  }

  private def templateElement(
      props: Props,
      setState: SetStateHookCallback[State],
      state: State,
      signingRequest: RevokeCredentialWithId
  ) = {

    val html = {
      // TODO: This needs to be validated before accepting the request, so that signingRequest.request.html is available
      io.iohk.atala.prism.credentials.Credential
        .fromString(signingRequest.request.signedCredentialStringRepresentation)
        .map(_.content)
        .flatMap { content =>
          content.credentialSubject
        }
        .toTry
        .flatMap { subject =>
          io.circe.parser.parse(subject).toTry
        }
        .flatMap { json =>
          json.hcursor.get[String]("html").toTry
        }
        .getOrElse("Unable to parse request, you likely want to reject it")
    }

    div(
      div(
        className := "credentialContainer",
        dangerouslySetInnerHTML := js.Dynamic.literal(__html = renderTemplate(html))
      ),
      br(),
      state.message.map(msg => AlertMessage(msg)),
      if (state.isLoading) {
        signatureElement(props, state, setState, signingRequest, "disabled")
      } else {
        signatureElement(props, state, setState, signingRequest, "")
      }
    )
  }

  private def signatureElement(
      props: Props,
      state: State,
      setState: SetStateHookCallback[State],
      signingRequest: RevokeCredentialWithId,
      appendClass: String
  ) = {
    div(className := "buttons_container")(
      rejectButton(props, setState, signingRequest, appendClass),
      nextElement(props, setState, state, signingRequest, appendClass),
      circularProgress(state)
    )
  }

  private def rejectButton(
      props: Props,
      setState: SetStateHookCallback[State],
      signingRequest: RevokeCredentialWithId,
      appendClass: String
  ) =
    div(
      className := s"btn_cancel btn_cancel_width $appendClass",
      id := "btn_cancel",
      "Reject operation",
      onClick := { () => rejectRequest(props, setState, signingRequest.id) }
    )

  private def signButton(
      props: Props,
      setState: SetStateHookCallback[State],
      signingRequest: RevokeCredentialWithId,
      appendClass: String
  ) = {
    val label = "Revoke credential"
    div(
      className := s"btn_sign btn_sign_width $appendClass",
      id := signingRequest.id.toString,
      label,
      onClick := { () =>
        signRequest(props, setState, signingRequest)
      }
    )
  }

  private def nextButton(props: Props, setState: SetStateHookCallback[State]) =
    div(
      className := s"btn_sign btn_sign_width",
      id := "next",
      "Next",
      onClick := { () =>
        nextCredential(props, setState)
      }
    )

  private def nextElement(
      props: Props,
      setState: SetStateHookCallback[State],
      state: State,
      signingRequest: RevokeCredentialWithId,
      appendClass: String
  ) = {
    state.status match {
      case Some(true) => nextButton(props, setState)
      case Some(false) | None => signButton(props, setState, signingRequest, appendClass)
    }
  }

  private def circularProgress(state: State) = {
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

  private def loadRequests(props: Props, setState: SetStateHookCallback[State]): Unit = {
    props.backgroundAPI.getRevocationRequests().map { req =>
      val revokeCredentials = req.requests
      if (revokeCredentials.nonEmpty)
        setState(_.copy(requests = revokeCredentials, id = 0, status = None))
      else props.switchToView(Main)
    }
  }

  private def nextCredential(props: Props, setState: SetStateHookCallback[State]): Unit = {
    loadRequests(props, setState)
  }

  private def signRequest(
      props: Props,
      setState: SetStateHookCallback[State],
      taggedRequest: RevokeCredentialWithId
  ): Unit = {
    setState(_.copy(isLoading = true))
    val result = props.backgroundAPI.approvePendingRequest(taggedRequest.id)
    val successMessage = "Credential successfully revoked!"
    val failureMessage = "Failed revoking credential."

    result.onComplete {
      case Success(_) =>
        setState(_.copy(status = Some(true), isLoading = false, message = Some(SuccessMessage(successMessage))))

      case Failure(ex) =>
        setState(_.copy(status = Some(false), isLoading = false, message = Some(FailMessage(failureMessage))))
        println(s"$failureMessage: ${ex.getMessage}")
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
            message = Some(SuccessMessage("Credential revocation successfully rejected."))
          )
        )
      case Failure(ex) =>
        setState(
          _.copy(status = Some(false), isLoading = false, message = Some(FailMessage("Credential failed to reject.")))
        )
        println(s"Failed rejecting credential : ${ex.getMessage}")
    }
  }

}
