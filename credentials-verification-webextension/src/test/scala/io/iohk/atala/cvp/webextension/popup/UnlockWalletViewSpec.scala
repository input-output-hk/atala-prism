package io.iohk.atala.cvp.webextension.popup

import io.iohk.atala.cvp.webextension.background.BackgroundAPI
import io.iohk.atala.cvp.webextension.background.wallet.Role
import io.iohk.atala.cvp.webextension.common.Mnemonic
import io.iohk.atala.cvp.webextension.testing.WalletDomSpec
import io.iohk.atala.cvp.webextension.testing.WalletTestHelper._
import org.scalajs.dom.raw.{HTMLElement, HTMLLabelElement}
import org.scalatest.concurrent.TestExecutionContext
import org.scalatest.matchers.must.Matchers._
import org.scalatest.wordspec.AsyncWordSpec
import scalatags.JsDom.all.div
import typings.std.document

import scala.concurrent.ExecutionContextExecutor

class UnlockWalletViewSpec extends AsyncWordSpec with WalletDomSpec {
  override implicit def executionContext: ExecutionContextExecutor = TestExecutionContext.runNow

  "UnlockView" should {
    "unlock wallet for valid password" in {
      withWallet {
        val container = div().render
        document.body.appendChild(container)
        val backgroundAPI = new BackgroundAPI()
        UnlockWalletView(backgroundAPI).unlock(container)
        setUpWallet {
          backgroundAPI.createWallet(PASSWORD, Mnemonic(), Role.Verifier, ORGANISATION_NAME, Array())
        }.flatMap { _ =>
          setInputValue("#password", PASSWORD)
          val unlockButton = document.querySelector("#unlockButton").asInstanceOf[HTMLElement]
          unlockButton.click()
          val statusLabel = document.querySelector("._label_update").asInstanceOf[HTMLLabelElement]

          futureResult {
            statusLabel.textContent must be("")
          }

        }
      }
    }

    "fail unlock on invalid password" in {
      withWallet {
        val backgroundAPI = new BackgroundAPI()
        val container = div().render
        document.body.appendChild(container)
        UnlockWalletView(backgroundAPI).unlock(container)
        setUpWallet {
          backgroundAPI
            .createWallet(PASSWORD, Mnemonic(), Role.Verifier, ORGANISATION_NAME, Array())
        }.flatMap { _ =>
          setInputValue("#password", "AWrongPassword")
          val unlockButton = document.querySelector("#unlockButton").asInstanceOf[HTMLElement]
          unlockButton.click()
          val statusLabel = document.querySelector("._label_update").asInstanceOf[HTMLLabelElement]

          futureResult {
            statusLabel.textContent must be("Invalid Password")
          }
        }
      }
    }
  }
}
