package io.iohk.atala.cvp.webextension.popup

import io.iohk.atala.cvp.webextension.background.BackgroundAPI
import io.iohk.atala.cvp.webextension.testing.WalletDomSpec
import org.scalatest.concurrent.TestExecutionContext
import org.scalatest.matchers.must.Matchers._
import org.scalatest.wordspec.AsyncWordSpec
import typings.std.{HTMLElement, document}
import io.iohk.atala.cvp.webextension.testing.WalletTestHelper._
import scala.concurrent.ExecutionContextExecutor

class InitialWalletViewSpec extends AsyncWordSpec with WalletDomSpec {
  override implicit def executionContext: ExecutionContextExecutor = TestExecutionContext.runNow

  override def beforeEach(): Unit = {
    super.beforeEach()

  }

  "InitialWalletView" should {
    "load recovery view" in {
      withWallet {
        val backgroundAPI = new BackgroundAPI()
        document.body = InitialWalletView(backgroundAPI).htmlBody
        val recoveryScreenButton = document.querySelector("#recoveryScreenButton").asInstanceOf[HTMLElement]

        recoveryScreenButton.click()
        futureResult {
          val recoveryScreen = document.querySelector("#recoveryScreen")
          recoveryScreen must not be null
        }
      }

    }

    "load registration view" in {
      withWallet {
        val backgroundAPI = new BackgroundAPI()
        document.body = InitialWalletView(backgroundAPI).htmlBody

        val registrationScreenButton = document.querySelector("#registrationScreenButton").asInstanceOf[HTMLElement]

        registrationScreenButton.click()
        futureResult {
          val registrationScreen = document.querySelector("#registrationScreen")
          registrationScreen must not be null
        }
      }
    }

    "load unlock wallet view" in {
      withWallet {
        val backgroundAPI = new BackgroundAPI()
        document.body = InitialWalletView(backgroundAPI).htmlBody
        val unlockScreenButton = document.querySelector("#unlockScreenButton").asInstanceOf[HTMLElement]

        unlockScreenButton.click()
        futureResult {
          val unlockScreen = document.querySelector("#unlockScreen")
          unlockScreen must not be null
        }
      }
    }
  }
}
