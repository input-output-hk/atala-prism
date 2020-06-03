package io.iohk.atala.cvp.webextension.activetab.context

import io.iohk.atala.cvp.webextension.Config
import io.iohk.atala.cvp.webextension.activetab.ActiveTabConfig
import io.iohk.atala.cvp.webextension.activetab.isolated.ExtensionAPI
import org.scalajs.dom

import scala.concurrent.ExecutionContext

/**
  * NOTE: This runs on the current web site context, which means we are in a risky environment
  * as the web site isn't controlled by us and it can re-define any js function to be different
  * to what we expect.
  */
class Runner(config: ActiveTabConfig, prismSdk: PrismSdk)(implicit
    ec: ExecutionContext
) {

  def run(): Unit = {
    log("This was run by the active tab on the web site context")
    prismSdk.inject(dom.window)
  }

  def log(msg: String): Unit = {
    println(s"prism: $msg")
  }
}

object Runner {

  def apply(config: Config)(implicit ec: ExecutionContext): Runner = {
    val extensionAPI = new ExtensionAPI()
    val prismSdk = new PrismSdk(extensionAPI = extensionAPI)
    new Runner(config.activeTabConfig, prismSdk)
  }
}
