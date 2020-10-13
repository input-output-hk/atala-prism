package io.iohk.atala.cvp.webextension.testing

import io.iohk.atala.cvp.webextension.Config
import io.iohk.atala.cvp.webextension.activetab.ActiveTabConfig
import io.iohk.atala.cvp.webextension.background.Runner
import io.iohk.atala.cvp.webextension.background.services.node.NodeClientService
import org.scalajs.dom.html
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, Suite}
import typings.std.{HTMLInputElement, document}
import scala.concurrent.ExecutionContextExecutor
import scala.scalajs.concurrent.JSExecutionContext
import scala.scalajs.js

trait WalletDomSpec extends Suite with BeforeAndAfterAll with BeforeAndAfterEach {

  private implicit def executionContext: ExecutionContextExecutor = JSExecutionContext.Implicits.queue

  override def beforeAll(): Unit = {
    // Fake the APIs not available during testing
    FakeApis.configure()
  }

  override def beforeEach(): Unit = {
    // Remove any listener from any previous test
    js.Dynamic.global.global.chrome.runtime.onMessage.removeAllListeners()

    // Delete any data stored by any previous test
    js.Dynamic.global.global.chrome.storage.local.clear()

    // Clear any DOM remaining from previous tests
    document.body.innerHTML = ""

    // Run the background script
    Runner(
      Config(
        ActiveTabConfig(List.empty),
        backendUrl = "http://localhost:10000/test",
        blockchainExplorerUrl = "http://localhost:10000/test-explorer",
        termsUrl = "http://localhost:10000/test-explorer",
        privacyPolicyUrl = "http://localhost:10000/test-explorer"
      ),
      FakeConnectorClientService,
      new NodeClientService("http://localhost:10000/test")
    ).run()

    // Add view to be tested
    document.body.appendChild(createHtmlUnderTest())
  }

  /**
    * Creates the HTML to be tested.
    */
  protected def createHtmlUnderTest(): html.Element = {
    document.createElement("div")
  }

  protected def setInputValue(inputSelector: String, value: String): Unit = {
    val input = document.querySelector(inputSelector).asInstanceOf[HTMLInputElement]
    input.value = value
  }
}
