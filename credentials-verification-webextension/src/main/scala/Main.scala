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

  private val config = if (io.iohk.atala.cvp.webextension.BuildInfo.production) {
    Config.Default
  } else {
    Config.Dev
  }

  def main(args: Array[String]): Unit = {
    // the main shouldn't do anything to avoid conflicts between contexts (tab, popup, background)
  }

  @JSExportTopLevel("runOnTab")
  def runOnTab(): Unit = {
    activetab.Runner(config).run()
  }

  @JSExportTopLevel("runOnBackground")
  def runOnBackground(): Unit = {
    background.Runner(config).run()
  }

  @JSExportTopLevel("runOnPopup")
  def runOnPopup(): Unit = {
    popup.Runner().run()
  }
}
