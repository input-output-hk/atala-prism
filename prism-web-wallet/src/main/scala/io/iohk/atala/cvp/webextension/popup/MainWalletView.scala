package io.iohk.atala.cvp.webextension.popup

import io.iohk.atala.cvp.webextension.background.BackgroundAPI
import io.iohk.atala.cvp.webextension.common.models.PendingRequest
import io.iohk.atala.cvp.webextension.popup.components.LockButton
import io.iohk.atala.cvp.webextension.popup.models.View.{ReviewCredentialIssuance, ReviewCredentialRevocation}
import io.iohk.atala.cvp.webextension.popup.models.{Message, View}
import slinky.core.FunctionalComponent
import slinky.core.annotations.react
import slinky.core.facade.{Hooks, SetStateHookCallback}
import slinky.web.html._

import scala.concurrent.ExecutionContext.Implicits.global

@react object MainWalletView {

  private val emptyDiv = div()()

  case class Props(backgroundAPI: BackgroundAPI, switchToView: (View) => Unit)

  case class State(
      requests: List[PendingRequest.WithId],
      message: Option[Message]
  )

  private def initialState: State = State(requests = Nil, None)

  val component = FunctionalComponent[Props] { props =>
    val (state, setState) = Hooks.useState[State](initialState)
    Hooks.useEffect(() => loadRequests(props, setState), "")

    if (state.requests.nonEmpty) {
      div(className := "generalContainer", id := "mainView")(
        div(className := "div_logo", id := "logoPrism", img(src := "/assets/images/prism-logo.svg")),
        div(
          className := "elementWrapper",
          p(className := "dashboardMainTitle", "Dashboard"),
          div(
            className := "dashboardBg",
            div(
              p(className := "dashboardTitle", "Your pending requests"),
              p(
                className := "greyText",
                "Click to sign or revoke your credentials."
              )
            ),
            pendingCredentialSigning(props, state),
            pendingRevocations(props, state),
            LockButton(props.backgroundAPI, message => onError(message, setState), props.switchToView)
          )
        )
      )
    } else {
      div(className := "generalContainer", id := "mainView")(
        div(className := "div_logo", id := "logoPrism", img(src := "/assets/images/prism-logo.svg")),
        p(className := "dashboardMainTitle", "Dashboard"),
        div(
          className := "dashboardBg",
          div(
            className := "img_NoRequest",
            img(className := "img-no-pending", src := "/assets/images/img-no-pending.png"),
            p(className := "dashboardTitle")(
              "There are no requests pending"
            )
          ),
          LockButton(props.backgroundAPI, message => onError(message, setState), props.switchToView)
        )
      )
    }
  }

  private def onError(message: Message, setState: SetStateHookCallback[State]): Unit = {
    setState(_.copy(message = Some(message)))
  }
  private def pendingRevocations(props: Props, state: State) = {
    val revocations = state.requests.collect {
      case r @ PendingRequest.WithId(_, PendingRequest.RevokeCredential(_, _, _, _)) => r
    }

    if (revocations.nonEmpty) { //TODO Style
      div(className := "signBoxContainer")(
        div(className := "dashboardContainer", onClick := { () => props.switchToView(ReviewCredentialRevocation) })(
          div(className := "_label_dashboard")(
            div(
              className := "flex",
              img(className := "successImg", src := "/assets/images/signatureIcon.svg"),
              div(
                className := "flexColumn",
                p(className := "noMargin", s"Revocation"),
                p(className := "greyText", s"${revocations.size}[credential(s)]")
              )
            ),
            img(className := "successImg", src := "/assets/images/boxarrow.svg")
          )
        )
      )
    } else {
      emptyDiv
    }
  }

  private def pendingCredentialSigning(props: Props, state: State) = {

    val issueCredentials = state.requests.collect {
      case r @ PendingRequest.WithId(_, PendingRequest.IssueCredential(_)) => r
    }

    if (issueCredentials.nonEmpty) {
      div(className := "signBoxContainer")(
        div(className := "dashboardContainer", onClick := { () => props.switchToView(ReviewCredentialIssuance) })(
          div(className := "_label_dashboard")(
            div(
              className := "flex",
              img(className := "successImg", src := "/assets/images/signatureIcon.svg"),
              div(
                className := "flexColumn",
                p(className := "noMargin", s"Signature"),
                p(className := "greyText", s"${issueCredentials.size}[credential(s)]")
              )
            ),
            img(className := "successImg", src := "/assets/images/boxarrow.svg")
          )
        )
      )
    } else {
      emptyDiv
    }
  }

  private def loadRequests(props: Props, setState: SetStateHookCallback[State]): Unit = {
    props.backgroundAPI.getSignatureRequests().map { req =>
      setState(_.copy(requests = req.requests))
    }
  }

}
