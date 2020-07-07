package io.iohk.atala.cvp.webextension.testing

import chrome.runtime.bindings.Runtime.AppID
import chrome.runtime.bindings.SendMessageOptions
import chrome.tabs.bindings.TabCreateProperties
import org.scalajs.dom

import scala.collection.mutable.ListBuffer
import scala.scalajs.js

object FakeChromeApi extends js.Object() {
  val storage: js.Object = new js.Object() {
    val local: js.Object = new js.Object() {
      val entries: js.Dictionary[js.Any] = new js.Object().asInstanceOf[js.Dictionary[js.Any]]

      def get(keys: js.UndefOr[js.Any] = js.undefined, callback: js.Function1[js.Dictionary[js.Any], _]): Unit = {
        callback(entries)
      }

      def set(items: js.Dictionary[js.Any], callback: js.UndefOr[js.Function0[_]] = js.undefined): Unit = {
        for ((key, value) <- items) {
          entries.put(key, value)
        }
        callback.map(fn => fn())
      }

      def clear(callback: js.UndefOr[js.Function0[_]] = js.undefined): Unit = {
        entries.clear()
        callback.map(fn => fn())
      }
    }
  }

  val tabs = new js.Object() {
    def create(tab: TabCreateProperties): Unit = {}
  }

  val runtime: js.Object = new js.Object() {
    val id = "id"

    val messageSender = new js.Object() {
      val url = "http://test.atalaprism.io/"
    }

    val listeners: ListBuffer[js.Function3[js.UndefOr[js.Any], js.Object, js.Function1[js.Any, _], Boolean]] =
      ListBuffer()

    val onMessage: js.Object = new js.Object() {

      def addListener(callback: js.Function3[js.UndefOr[js.Any], js.Object, js.Function1[js.Any, _], Boolean]): Unit = {
        listeners += callback
      }
      def removeListener(
          callback: js.Function3[js.UndefOr[js.Any], js.Object, js.Function1[js.Any, _], Boolean]
      ): Unit = {
        listeners -= callback
      }
      def hasListener(
          callback: js.Function3[js.UndefOr[js.Any], js.Object, js.Function1[js.Any, _], Boolean]
      ): Boolean = {
        listeners.contains(callback)
      }
      def hasListeners(): Boolean = {
        listeners.nonEmpty
      }
      def removeAllListeners(): Unit = {
        listeners.clear()
      }
    }

    def sendMessage(
        extensionId: js.UndefOr[AppID] = js.undefined,
        message: js.Any,
        options: js.UndefOr[SendMessageOptions] = js.undefined,
        responseCallback: js.UndefOr[js.Function1[js.Any, _]] = js.undefined
    ): Unit = {
      listeners foreach { listener =>
        listener(message, messageSender, responseCallback.get)
      }
    }
  }

  val browserAction: js.Object = new js.Object() {
    def setPopup(details: js.Any, callback: js.UndefOr[js.Function0[_]] = js.undefined): Unit = {}
  }
}
