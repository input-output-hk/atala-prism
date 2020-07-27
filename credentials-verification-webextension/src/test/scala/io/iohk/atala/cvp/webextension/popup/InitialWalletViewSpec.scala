package io.iohk.atala.cvp.webextension.popup

import io.iohk.atala.cvp.webextension.background.BackgroundAPI
import io.iohk.atala.cvp.webextension.testing.WalletDomSpec
import org.scalatest.matchers.must.Matchers._
import org.scalatest.wordspec.AnyWordSpec
import typings.std.{HTMLElement, document}

import scala.concurrent.ExecutionContextExecutor
import scala.scalajs.concurrent.JSExecutionContext

class InitialWalletViewSpec extends AnyWordSpec with WalletDomSpec {
  implicit def executionContext: ExecutionContextExecutor = JSExecutionContext.Implicits.queue

  override def beforeEach(): Unit = {
    super.beforeEach()
    document.body = InitialWalletView(new BackgroundAPI()).htmlBody
  }

  "InitialWalletView" should {
    "load recovery view" in {
      val recoveryScreenButton = document.querySelector("#recoveryScreenButton").asInstanceOf[HTMLElement]

      recoveryScreenButton.click()

      val recoveryScreen = document.querySelector("#recoveryScreen")
      recoveryScreen must not be null
    }

    "load registration view" in {
      val registrationScreenButton = document.querySelector("#registrationScreenButton").asInstanceOf[HTMLElement]

      registrationScreenButton.click()

      val registrationScreen = document.querySelector("#registrationScreen")
      registrationScreen must not be null
    }

    "load unlock wallet view" in {
      val unlockScreenButton = document.querySelector("#unlockScreenButton").asInstanceOf[HTMLElement]

      unlockScreenButton.click()

      val unlockScreen = document.querySelector("#unlockScreen")
      unlockScreen must not be null
    }
  }
}
