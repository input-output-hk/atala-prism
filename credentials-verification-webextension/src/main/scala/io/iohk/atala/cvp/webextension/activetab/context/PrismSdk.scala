package io.iohk.atala.cvp.webextension.activetab.context

import io.iohk.atala.cvp.webextension.activetab.isolated.ExtensionAPI

import scala.concurrent.ExecutionContext
import scala.scalajs.js
import scala.scalajs.js.JSConverters._
import scala.scalajs.js.PropertyDescriptor

/**
  * This is the sdk object that can be accessed from any website to interact with our extension.
  *
  * There are important details to take care on the exposed functions:
  * - They should be JavaScript friendly instead of Scala, for example, use Promise instead of Future.
  * - They shouldn't expose the internal messaging representation.
  * - The Scala functions defined (like getWalletStatus) should be public, otherwise, the compiler fails.
  */
class PrismSdk(name: String = "prism", extensionAPI: ExtensionAPI)(implicit
    ec: ExecutionContext
) {

  def inject(parent: js.Object): Unit = {
    js.Object.defineProperty(
      parent,
      name,
      new PropertyDescriptor {
        enumerable = false
        writable = false
        configurable = false
        // NOTE: The highlighted error is a bug on IntelliJ as the code compiles properly
        value = js.Dictionary(
          "log" -> js.Any.fromFunction1(log), // TODO: Remove
          "getWalletStatus" -> js.Any.fromFunction0(getWalletStatus)
        )
      }
    )
  }

  def getWalletStatus(): js.Promise[String] = {
    extensionAPI.getWalletStatus().map(_.status).toJSPromise
  }

  def log(text: String): Unit = {
    println(s"PrismSdk: $text")
  }
}
