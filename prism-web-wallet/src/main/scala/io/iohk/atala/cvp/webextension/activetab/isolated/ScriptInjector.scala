package io.iohk.atala.cvp.webextension.activetab.isolated

import org.scalajs.dom

import scala.concurrent.{Future, Promise}

private[isolated] class ScriptInjector {

  /**
    * Content scripts run in an isolated world, see https://developer.chrome.com/extensions/content_scripts
    *
    * The way to communicate the content script and the web page is by sending messages, which gets ugly
    * very easily, hence, we provide a small SDK to abstract such communication.
    *
    * When a web site is willing to interact with our extension, it can check whether our SDK available, then
    * the JavaScript functions could be invoked directly.
    *
    * We would like to inject the JavaScript API that could be invoked directly from the web sites.
    *
    * NOTE: Metamask has done a great job on making a robust script which we may integrate eventually:
    * - https://github.com/MetaMask/metamask-extension/blob/develop/app/scripts/contentscript.js
    */
  def injectPrivilegedScript(scriptFile: String): Future[Unit] = {
    val promise = Promise[Unit]()
    val script = dom.document.createElement("script").asInstanceOf[org.scalajs.dom.raw.HTMLScriptElement]
    // NOTE: This script must be included on the web_accessible_resources in manifest.json
    script.src = chrome.runtime.Runtime.getURL(scriptFile)
    script.onload = _ => {
      script.parentNode.removeChild(script)
      promise.success(())
    }

    Option(dom.document.head)
      .getOrElse(dom.document.documentElement)
      .appendChild(script)
    promise.future
  }
}
