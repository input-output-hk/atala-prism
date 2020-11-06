package io.iohk.atala.cvp.webextension.common.services

import chrome.windows.bindings.{CreateOptions, UpdateOptions, Window}
import typings.std.global.screen

import scala.concurrent.ExecutionContext
import scala.scalajs.js

class BrowserWindowService {
  private var windowId: Option[Int] = None

  def createOrUpdate(mayBeParam: Option[String] = None)(implicit ec: ExecutionContext): Unit = {

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
        chrome.windows.Windows.update(id, UpdateOptions(focused = true))
      }
      .getOrElse {
        chrome.windows.Windows.create(options).map {
          _.map { window =>
            windowId = window.id.toOption
            chrome.windows.Windows.onRemoved.listen { id =>
              if (window.id.toOption.contains(id)) windowId = None
            }
          }
        }
      }

  }
}

object BrowserWindowService {
  def apply(): BrowserWindowService = new BrowserWindowService()
}
