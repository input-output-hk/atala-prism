import io.iohk.atala.cvp.webextension._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.scalajs.js.annotation.JSExportTopLevel

/**
  * Entry-point for any context, it loads the config based on the current environment, which can be
  * production or development for now.
  *
  * It creates the necessary objects and execute the runner for the actual context.
  *
  * It is assumed that the entry-point is an actual JavaScript file that invokes these functions.
  */
object Main {

  private val config = if (BuildInfo.production) {
    Config.default(BuildInfo.activeTabContextScripts, overrideConnectorUrl = BuildInfo.overrideConnectorUrl)
  } else {
    Config.dev(BuildInfo.activeTabContextScripts, overrideConnectorUrl = BuildInfo.overrideConnectorUrl)
  }

  def main(args: Array[String]): Unit = {
    // the main shouldn't do anything to avoid conflicts between contexts (tab, popup, background)
  }

  @JSExportTopLevel("runOnTab")
  def runOnTab(): Unit = {
    activetab.isolated.Runner(config).run()
  }

  @JSExportTopLevel("runOnCurrentTabContext")
  def runOnCurrentTabContext(): Unit = {
    activetab.context.Runner(config).run()
  }

  @JSExportTopLevel("runOnBackground")
  def runOnBackground(): Unit = {
    logVersionInfo()
    background.Runner(config).run()
  }

  @JSExportTopLevel("runOnPopup")
  def runOnPopup(): Unit = {
    popup.Runner(config).run()
  }

  private def logVersionInfo(): Unit = {
    org.scalajs.dom.console.log(s"Prism Wallet is running, $BuildInfo")
  }
}
