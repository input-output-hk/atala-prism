package io.iohk.atala.cvp.webextension.common.services

import chrome.windows.bindings.{CreateOptions, Window}
import typings.std.global.screen

import scala.concurrent.{ExecutionContext, Future}
import scala.scalajs.js

class BrowserWindowService {

  def createOrUpdate(mayBeParam: Option[String] = None, windowId: Option[Window.Id] = None)(implicit
      ec: ExecutionContext
  ): Future[Option[Window]] = {

    val param = mayBeParam.map(p => s"?view=$p").getOrElse("")
    val url = s"chrome-extension://${chrome.runtime.Runtime.id}/popup.html$param"

    /**
      * screen top,left (0,0)
      * Screen height x , Screen width y
      * window height 700 , window width 375
      * to position the window to the center from top we move it rightCenter position i.e. (screen.height / 2) - (windowHeight / 2)
      * to position the window to the center from left we move it leftCenter position i.e. (screen.width / 2) - (windowWidth / 2) (
      * we need the window not in center but towards more right in screen from center i.e. leftCenter + leftCenter / 2
      */
    val windowWidth = 375
    val windowHeight = 700
    val leftCenter = (screen.width / 2) - (windowWidth / 2)
    val left = leftCenter + leftCenter / 2
    val top = (screen.height / 2) - (windowHeight / 2)

    val options =
      CreateOptions(
        url = js.Array(url),
        `type` = Window.Type.POPUP,
        focused = true,
        width = windowWidth,
        height = windowHeight,
        left = left.toInt,
        top = top.toInt
      )

    windowId
      .map { id =>
        chrome.windows.Windows.remove(id).flatMap { _ =>
          chrome.windows.Windows.create(options)
        }
      }
      .getOrElse {
        chrome.windows.Windows.create(options)
      }
  }
}

object BrowserWindowService {
  def apply(): BrowserWindowService = new BrowserWindowService()
}
