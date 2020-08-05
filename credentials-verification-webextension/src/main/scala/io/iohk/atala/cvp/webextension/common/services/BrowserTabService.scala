package io.iohk.atala.cvp.webextension.common.services

import chrome.tabs.bindings.{ReloadProperties, TabCreateProperties}

import scala.concurrent.ExecutionContext
import scala.scalajs.js
import scala.scalajs.js.JSConverters._

class BrowserTabService {
  var tabId: Option[Int] = None

  def createOrUpdateTab(mayBeParam: Option[String] = None)(implicit ec: ExecutionContext): Unit = {
    val param = mayBeParam
      .map(p => s"?view=$p")
      .getOrElse("")

    val url = s"chrome-extension://${chrome.runtime.Runtime.id}/popup.html$param"
    val tabProperties = TabCreateProperties(url = url)
    tabId
      .map { id =>
        chrome.tabs.Tabs.reload(id, ReloadProperties(Option(true).orUndefined))
      }
      .getOrElse {
        chrome.tabs.Tabs.create(tabProperties).map { x =>
          chrome.tabs.Tabs.onRemoved.listen {
            case (id, _) if x.id.toOption.contains(id) => tabId = None
          }
          tabId = x.id.toOption
        }
      }
  }

}

object BrowserTabService {
  def apply(): BrowserTabService = new BrowserTabService()
}
