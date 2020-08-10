package io.iohk.atala.cvp.webextension.popup

import io.iohk.atala.cvp.webextension.background.BackgroundAPI
import org.scalajs.dom.html.{Body, Div}
import org.scalajs.dom.raw.Node
import scalatags.JsDom.all.{div, id, _}

import scala.concurrent.{ExecutionContext, Future}

class MainWalletView(backgroundAPI: BackgroundAPI)(implicit ec: ExecutionContext) {

  def mainWalletScreen(divElement: Div): Future[Node] = {

    divElement.clear()
    renderRequests(divElement).map { _ =>
      divElement
    }
  }

  private def renderRequests(divElement: Div): Future[Unit] = {

    val cancelButton = div(cls := "btn_cancel", id := "btn_cancel")("Cancel").render

    backgroundAPI.getSignatureRequests().map { req =>
      if (req.requests.nonEmpty) {
        for (x <- req.requests) yield {
          val signRequestDiv = div(
            cls := "btn_sign",
            id := x.id,
            "Sign",
            onclick := { () =>
              backgroundAPI.signRequestAndPublish(x.id)
            }
          )

          val pendingRequestScreen = {
            div(cls := "status_container", id := "mainView")(
              h3(cls := "h3_pending")(
                "Signature request"
              ),
              p(
                cls := "description_signature",
                id := "description_signature",
                "You have been requested to sign the following credential:"
              )
            )
          }.render

          val divSignAndCancel = div(cancelButton, signRequestDiv).render
          divElement.appendChild(pendingRequestScreen)
          divElement.appendChild(divSignAndCancel)
        }
      } else {
        val noRequestsScreen = {
          div(cls := "no-pending-container", id := "mainView")(
            div(cls := "img-no-pending")(img(src := "/assets/images/img-no-pending.png")),
            p(cls := "welcome_text")(
              "There are not pending requests"
            ),
            div(cls := "lock_button")(
              div(cls := "img_lock")(
                img(src := "/assets/images/padlock.png")
              ),
              div(
                p(cls := "txt_lock_button")("Lock your account")
              )
            )
          )
        }.render
        divElement.appendChild(noRequestsScreen)
      }

    }
  }

  def htmlBody: Body = {
    val containerDiv: Div = div(
      cls := "container",
      id := "containerId"
    ).render

    mainWalletScreen(containerDiv)

    val htmlBody = body(
      link(rel := "stylesheet", href := "css/popup.css"),
      script(src := "scripts/common.js"),
      script(src := "main-bundle.js"),
      script(src := "scripts/popup-script.js"),
      containerDiv.render
    ).render

    htmlBody

  }
}

object MainWalletView {
  def apply(backgroundAPI: BackgroundAPI)(implicit ec: ExecutionContext): MainWalletView =
    new MainWalletView(backgroundAPI)
}
