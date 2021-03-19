package io.iohk.atala.cvp.webextension.popup

import com.alexitc.materialui.facade.materialUiCore.anon.PartialClassNameMapCircul
import com.alexitc.materialui.facade.materialUiCore.materialUiCoreStrings.indeterminate
import com.alexitc.materialui.facade.materialUiCore.{components => mui}
import com.alexitc.materialui.facade.materialUiIcons.{components => muiIcons}
import io.iohk.atala.cvp.webextension.background.BackgroundAPI
import io.iohk.atala.cvp.webextension.common.models.PendingRequest
import io.iohk.atala.cvp.webextension.popup.components.AlertMessage
import io.iohk.atala.cvp.webextension.popup.models.Message.{FailMessage, SuccessMessage}
import io.iohk.atala.cvp.webextension.popup.models.View.Unlock
import io.iohk.atala.cvp.webextension.popup.models.{Message, View}
import org.scalajs.dom.raw.DOMParser
import slinky.core.FunctionalComponent
import slinky.core.annotations.react
import slinky.core.facade.{Hooks, ReactElement, SetStateHookCallback}
import slinky.web.html._
import typings.dompurify.mod.{^ => dompurify}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.scalajs.js
import scala.util.{Failure, Success}

@react object MainWalletView {

  private val domParser = new DOMParser()
  private val emptyDiv = "<div/>"

  case class Props(backgroundAPI: BackgroundAPI, switchToView: (View) => Unit)

  case class State(
      requests: List[PendingRequest.WithId],
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

    if (state.requests.nonEmpty) {
      val signingRequest = state.requests(state.id)

      div(id := "mainView", className := "spaceBetween")(
        div(
          className := "div_logo",
          id := "logoPrism",
          img(className := "logoImage", src := "/assets/images/prism-logo.svg")
        ),
        h3(className := "h3_pending")(
          "Signature request"
        ),
        p(
          className := "description_signature",
          id := "description_signature",
          "You have been requested to sign the following operations:"
        ),
        templateElement(props, setState, state, signingRequest),
        pagingElement(state, setState),
        lockButton(props, setState)
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
        lockButton(props, setState)
      )
    }
  }

  private def templateElement(
      props: Props,
      setState: SetStateHookCallback[State],
      state: State,
      signingRequest: PendingRequest.WithId
  ) = {
    val html = signingRequest.request match {
      case r: PendingRequest.IssueCredential =>
        // TODO: This needs to be validated before accepting the request, so that r.html is available
        r.credentialData.properties.getOrElse("html", emptyDiv)

      case r: PendingRequest.RevokeCredential =>
        // TODO: This needs to be validated before accepting the request, so that r.html is available
        io.iohk.atala.prism.credentials.Credential
          .fromString(r.signedCredentialStringRepresentation)
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
      signingRequest: PendingRequest.WithId,
      appendClass: String
  ) = {
    div(className := "buttons_container")(
      rejectButton(props, setState, signingRequest, appendClass),
      nextElement(props, setState, state, signingRequest, appendClass),
      circularProgress(state)
    )
  }

  private def pagingElement(state: State, setState: SetStateHookCallback[State]): ReactElement = {
    val count = state.requests.size
    val previous = math.max(state.id - 1, 0)
    val next = math.min(state.id + 1, count - 1)

    div(className := "div__field_group_mui")(
      mui.IconButton.onClick(_ => setState(_.copy(id = previous, status = None)))(muiIcons.ChevronLeftOutlined()),
      s"${state.id + 1} of $count",
      mui.IconButton.onClick(_ => setState(_.copy(id = next, status = None)))(muiIcons.ChevronRightOutlined())
    )
  }

  private def rejectButton(
      props: Props,
      setState: SetStateHookCallback[State],
      signingRequest: PendingRequest.WithId,
      appendClass: String
  ) =
    div(
      className := s"btn_cancel btn_cancel_width $appendClass",
      id := "btn_cancel",
      "Reject operation",
      onClick := { () => rejectRequest(props, setState, signingRequest.id) }
    )

  private def lockButton(props: Props, setState: SetStateHookCallback[State]) =
    div(className := "div__field_group")(
      div(className := "lock_button", onClick := { () => lockWallet(props, setState) })(
        div(className := "img_lock")(
          img(src := "/assets/images/padlock.png")
        ),
        div(
          p(className := "txt_lock_button")("Lock your account")
        )
      )
    )

  private def signButton(
      props: Props,
      setState: SetStateHookCallback[State],
      signingRequest: PendingRequest.WithId,
      appendClass: String
  ) = {
    val label = signingRequest.request match {
      case _: PendingRequest.IssueCredential => "Issue credential"
      case _: PendingRequest.RevokeCredential => "Revoke credential"
    }
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
      signingRequest: PendingRequest.WithId,
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
    props.backgroundAPI.getSignatureRequests().map { req =>
      setState(_.copy(requests = req.requests, status = None))
    }
  }

  private def nextCredential(props: Props, setState: SetStateHookCallback[State]): Unit = {
    loadRequests(props, setState)
  }

  private def signRequest(
      props: Props,
      setState: SetStateHookCallback[State],
      taggedRequest: PendingRequest.WithId
  ): Unit = {
    setState(_.copy(isLoading = true))
    val result = props.backgroundAPI.approvePendingRequest(taggedRequest.id)

    val (successMessage, failureMessage) = taggedRequest.request match {
      case _: PendingRequest.IssueCredential => ("Credential successfully signed!", "Credential signing failed.")
      case _: PendingRequest.RevokeCredential => ("Credential successfully revoked!", "Failed revoking credential.")
    }

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
            message = Some(SuccessMessage("Credential successfully rejected."))
          )
        )
      case Failure(ex) =>
        setState(
          _.copy(status = Some(false), isLoading = false, message = Some(FailMessage("Credential failed to reject.")))
        )
        println(s"Failed Publishing Credential : ${ex.getMessage}")
    }
  }

  private def lockWallet(props: Props, setState: SetStateHookCallback[State]): Unit = {
    props.backgroundAPI.lockWallet().onComplete {
      case Success(_) => props.switchToView(Unlock)
      case Failure(ex) =>
        setState(_.copy(message = Some(FailMessage("Failed Locking wallet."))))
        println(s"Failed Locking wallet : ${ex.getMessage}")
    }
  }
}
