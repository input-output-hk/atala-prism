package io.iohk.atala.cvp.webextension.activetab.isolated

import io.iohk.atala.cvp.webextension.Config
import io.iohk.atala.cvp.webextension.activetab.ActiveTabConfig
import io.iohk.atala.cvp.webextension.background.BackgroundAPI

import scala.concurrent.{ExecutionContext, Future}

class Runner(
    config: ActiveTabConfig,
    scriptInjector: ScriptInjector,
    externalMessageProcessor: ExternalMessageProcessor
)(implicit
    ec: ExecutionContext
) {

  def run(): Unit = {
    log("This was run by the active tab")
    externalMessageProcessor.start()
    injectPrivilegedScripts(config.contextScripts)
      .foreach { _ =>
        log("Scripts injected, the web site context should start soon")
      }
  }

  private def injectPrivilegedScripts(scripts: Seq[String]): Future[Unit] = {
    // it's important to load the scripts in the right order
    scripts.foldLeft(Future.unit) {
      case (acc, cur) =>
        acc.flatMap(_ => scriptInjector.injectPrivilegedScript(cur))
    }
  }

  private def log(msg: String): Unit = {
    println(s"activeTab: $msg")
  }
}

object Runner {

  def apply(config: Config)(implicit ec: ExecutionContext): Runner = {
    val backgroundAPI = new BackgroundAPI()
    val commandProcessor = new CommandProcessor(backgroundAPI)
    val externalMessageProcessor = new ExternalMessageProcessor(commandProcessor)
    val scriptInjector = new ScriptInjector
    new Runner(config.activeTabConfig, scriptInjector, externalMessageProcessor)
  }
}
