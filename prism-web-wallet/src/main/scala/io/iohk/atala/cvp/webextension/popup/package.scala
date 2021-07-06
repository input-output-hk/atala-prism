package io.iohk.atala.cvp.webextension

import org.scalajs.dom.raw.HTMLElement

package object popup {

  implicit class HtmlElementExt(element: HTMLElement) {

    def clear(): Unit = {
      while (element.hasChildNodes()) {
        element.removeChild(element.firstChild)
      }
    }

  }
}
