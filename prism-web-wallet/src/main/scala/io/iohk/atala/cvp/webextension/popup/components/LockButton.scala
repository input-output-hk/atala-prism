package io.iohk.atala.cvp.webextension.popup.components

import io.iohk.atala.cvp.webextension.background.BackgroundAPI
import io.iohk.atala.cvp.webextension.popup.models.Message.FailMessage
import io.iohk.atala.cvp.webextension.popup.models.View.Unlock
import io.iohk.atala.cvp.webextension.popup.models.{Message, View}
import slinky.core._
import slinky.core.annotations.react
import slinky.web.html._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

@react object LockButton {
  case class Props(backgroundAPI: BackgroundAPI, onError: Message => Unit, switchToView: (View) => Unit)

  val component: FunctionalComponent[Props] = FunctionalComponent[Props] { props =>
    div(className := "div__field_group")(
      div(className := "lock_button", onClick := { () => lockWallet(props) })(
        div(className := "img_lock")(
          img(src := "/assets/images/padlock.png")
        ),
        div(
          p(className := "txt_lock_button")("Lock your account")
        )
      )
    )
  }

  private def lockWallet(props: Props): Unit = {
    props.backgroundAPI.lockWallet().onComplete {
      case Success(_) => props.switchToView(Unlock)
      case Failure(ex) => {
        props.onError(FailMessage("Failed Locking wallet."))
        println(s"Failed Locking wallet : ${ex.getMessage}")
      }
    }
  }
}
