package io.iohk.atala.cvp.webextension.background.services.browser

import chrome.tabs.bindings.{ReloadProperties, TabCreateProperties}

import scala.concurrent.ExecutionContext
import scala.scalajs.js
import js.JSConverters._

class BrowserTabService {
  var tabId: Option[Int] = None

  def createOrUpdateTab(implicit ec: ExecutionContext): Unit = {
    val url = s"chrome-extension://${chrome.runtime.Runtime.id}/popup.html"
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
