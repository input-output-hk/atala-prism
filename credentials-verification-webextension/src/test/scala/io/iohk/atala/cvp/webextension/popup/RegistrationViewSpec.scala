package io.iohk.atala.cvp.webextension.popup

import io.iohk.atala.cvp.webextension.background.BackgroundAPI
import io.iohk.atala.cvp.webextension.testing.WalletDomSpec
import org.scalatest.matchers.must.Matchers._
import org.scalatest.wordspec.AnyWordSpec
import scalatags.JsDom.all.div
import typings.std.{HTMLElement, HTMLLabelElement, document}

import scala.concurrent.ExecutionContextExecutor
import scala.scalajs.concurrent.JSExecutionContext

class RegistrationViewSpec extends AnyWordSpec with WalletDomSpec {
  implicit def executionContext: ExecutionContextExecutor = JSExecutionContext.Implicits.queue

  override def beforeEach(): Unit = {
    super.beforeEach()

    val container = div().render
    document.body.appendChild(container)
    RegistrationView(new BackgroundAPI()).registrationScreen(container)
  }

  "RegistrationView" should {
    "work with valid input" in {
      setInputValue("#password", "apassword")
      setInputValue("#password2", "apassword")
      setInputValue("#orgname", "IOHK")
      val statusLabel = document.querySelector("._label_update").asInstanceOf[HTMLLabelElement]
      val registerButton = document.querySelector("#registerButton").asInstanceOf[HTMLElement]

      registerButton.click()

      statusLabel.textContent must be("")
      // TODO: Test wallet has been created successfully
    }

    "fail on empty password" in {
      setInputValue("#password", "")
      setInputValue("#password2", "apasswordthatdoesnotmatch")
      setInputValue("#orgname", "IOHK")
      val statusLabel = document.querySelector("._label_update").asInstanceOf[HTMLLabelElement]
      val registerButton = document.querySelector("#registerButton").asInstanceOf[HTMLElement]

      registerButton.click()

      statusLabel.textContent must be("Password cannot be empty")
    }

    "fail on password mismatch" in {
      setInputValue("#password", "apassword")
      setInputValue("#password2", "apasswordthatdoesnotmatch")
      setInputValue("#orgname", "IOHK")
      val statusLabel = document.querySelector("._label_update").asInstanceOf[HTMLLabelElement]
      val registerButton = document.querySelector("#registerButton").asInstanceOf[HTMLElement]

      registerButton.click()

      statusLabel.textContent must be("Password verification does not match")
    }

    // TODO: Test logo file upload
  }
}
