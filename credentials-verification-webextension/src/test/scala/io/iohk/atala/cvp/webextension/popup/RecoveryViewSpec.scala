package io.iohk.atala.cvp.webextension.popup

import io.iohk.atala.cvp.webextension.background.BackgroundAPI
import io.iohk.atala.cvp.webextension.testing.WalletDomSpec
import org.scalatest.matchers.must.Matchers._
import org.scalatest.wordspec.AnyWordSpec
import scalatags.JsDom.all.div
import typings.std.{HTMLElement, HTMLLabelElement, document}

import scala.concurrent.ExecutionContextExecutor
import scala.scalajs.concurrent.JSExecutionContext

class RecoveryViewSpec extends AnyWordSpec with WalletDomSpec {
  implicit def executionContext: ExecutionContextExecutor = JSExecutionContext.Implicits.queue

  val VALID_SEED_PHRASE = "describe young short dirt wife found mule response garlic stadium soldier struggle"

  override def beforeEach(): Unit = {
    super.beforeEach()

    val container = div().render
    document.body.appendChild(container)
    RecoveryView(new BackgroundAPI()).recover(container)
  }

  "RecoveryView" should {
    "work with valid input" in {
      setInputValue("#seedphrase", VALID_SEED_PHRASE)
      setInputValue("#password", "apassword")
      setInputValue("#password2", "apassword")
      val statusLabel = document.querySelector("._label_update").asInstanceOf[HTMLLabelElement]
      val recoverButton = document.querySelector("#recoverButton").asInstanceOf[HTMLElement]

      recoverButton.click()

      statusLabel.textContent must be("")
      // TODO: Test wallet has been recovered successfully
    }

    "fail on empty password" in {
      setInputValue("#seedphrase", VALID_SEED_PHRASE)
      setInputValue("#password", "")
      setInputValue("#password2", "apasswordthatdoesnotmatch")
      val statusLabel = document.querySelector("._label_update").asInstanceOf[HTMLLabelElement]
      val recoverButton = document.querySelector("#recoverButton").asInstanceOf[HTMLElement]

      recoverButton.click()

      statusLabel.textContent must be("Password cannot be empty")
    }

    "fail on password mismatch" in {
      setInputValue("#seedphrase", VALID_SEED_PHRASE)
      setInputValue("#password", "apassword")
      setInputValue("#password2", "apasswordthatdoesnotmatch")
      val statusLabel = document.querySelector("._label_update").asInstanceOf[HTMLLabelElement]
      val recoverButton = document.querySelector("#recoverButton").asInstanceOf[HTMLElement]

      recoverButton.click()

      statusLabel.textContent must be("Password verification does not match")
    }

    // TODO: Test invalid seed phrase
  }
}
