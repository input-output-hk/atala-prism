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
      div(className := "spaceBetween", id := "mainView")(
        div(className := "div_logo", id := "logoPrism", img(src := "/assets/images/prism-logo.svg")),
        div(
          p(className := "welcome_text")(
            "Your pending requests"
          ),
          pendingCredentialSigning(props, state),
          pendingRevocations(props, state)
        ),
        LockButton(props.backgroundAPI, message => onError(message, setState), props.switchToView)
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
        LockButton(props.backgroundAPI, message => onError(message, setState), props.switchToView)
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
      div(className := "div__field_group")(
        div(className := "dashboardContainer", onClick := { () => props.switchToView(ReviewCredentialRevocation) })(
          label(className := "_label_dashboard")(
            s"Revocation [${revocations.size} credential(s)]",
            img(className := "successImg", src := "/assets/images/success.svg")
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
      div(className := "div__field_group")( //TODO Style
        div(className := "dashboardContainer", onClick := { () => props.switchToView(ReviewCredentialIssuance) })(
          label(className := "_label_dashboard")(
            s"Signature [${issueCredentials.size} credential(s)]",
            img(className := "successImg", src := "/assets/images/success.svg")
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
