package io.iohk.atala.cvp.webextension.popup

import io.iohk.atala.cvp.webextension.background.BackgroundAPI
import org.scalajs.dom.html.{Body, Div}
import org.scalajs.dom.raw.Node
import scalatags.JsDom.all.{div, id, _}

import scala.concurrent.{ExecutionContext, Future}

class MainWalletView(backgroundAPI: BackgroundAPI)(implicit ec: ExecutionContext) {

  def mainWalletScreen(divElement: Div): Future[Node] = {

    val mainScreen = {
      div(cls := "status_container", id := "mainView")(
        h3(cls := "h3_pending")(
          "Pending request"
        )
      )
    }.render

    divElement.clear()
    divElement.appendChild(mainScreen)
    renderRequests(divElement).map { _ =>
      divElement
    }
  }

  private def renderRequests(divElement: Div): Future[List[Node]] = {
    backgroundAPI.getSignatureRequests().map { req =>
      for (x <- req.requests) yield {
        val signRequestDiv = div(
          cls := "div__btn",
          id := x.id,
          x.subject.id,
          onclick := { () =>
            backgroundAPI.signRequestAndPublish(x.id)
          }
        )
        divElement.appendChild(signRequestDiv.render)
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
