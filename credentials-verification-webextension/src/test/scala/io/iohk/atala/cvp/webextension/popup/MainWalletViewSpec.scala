package io.iohk.atala.cvp.webextension.popup

import io.iohk.atala.cvp.webextension.background.BackgroundAPI
import io.iohk.atala.cvp.webextension.background.wallet.Role
import io.iohk.atala.cvp.webextension.common.Mnemonic
import io.iohk.atala.cvp.webextension.common.models.CredentialSubject
import io.iohk.atala.cvp.webextension.popup.Wallet._
import io.iohk.atala.cvp.webextension.testing.WalletDomSpec
import org.scalajs.dom.raw.HTMLDivElement
import org.scalatest.Assertion
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.must.Matchers._
import org.scalatest.wordspec.AsyncWordSpec
import scalatags.JsDom.all.div
import typings.std.document

import scala.concurrent.Future

class MainWalletViewSpec extends AsyncWordSpec with WalletDomSpec with ScalaFutures {

  override implicit def executionContext =
    org.scalatest.concurrent.TestExecutionContext.runNow

  override def beforeEach(): Unit = {
    super.beforeEach()

  }

  "MainWalletView" should {

    "load Main view with pending requests" in {
      withWallet {
        val backgroundAPI = new BackgroundAPI()
        val did = "did:prism:sign-me"
        val subject = CredentialSubject(did, Map("key" -> "value"))
        setUpWallet {
          for {
            _ <- backgroundAPI.createWallet(PASSWORD, Mnemonic(), Role.Verifier, ORGANISATION_NAME, Array())
            u <- backgroundAPI.login()
            _ <- backgroundAPI.requestSignature(u.sessionId, subject)
          } yield ()
        }.flatMap { _ =>
          val container = div().render
          document.body.appendChild(container)
          MainWalletView(backgroundAPI).mainWalletScreen(container).map { _ =>
            val divElement = document
              .querySelector(".div__btn")
              .asInstanceOf[HTMLDivElement]
            divElement must not be null
            divElement.textContent mustBe "did:prism:sign-me"
          }
        }
      }
    }

  }
}

object Wallet {
  val PASSWORD = "test-password"
  val TEST_KEY = "test-key"
  val ORGANISATION_NAME = "IOHK"

  def setUpWallet(f: => Future[Unit]): Future[Unit] = {
    f
  }

  def withWallet(test: => Future[Assertion]): Future[Assertion] = {
    try {
      test
    } finally {
      println("Test complete")
    }
  }

}
