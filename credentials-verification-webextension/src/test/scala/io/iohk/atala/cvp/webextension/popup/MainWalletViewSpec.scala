package io.iohk.atala.cvp.webextension.popup

import io.iohk.atala.cvp.webextension.background.BackgroundAPI
import io.iohk.atala.cvp.webextension.background.wallet.Role
import io.iohk.atala.cvp.webextension.common.Mnemonic
import io.iohk.atala.cvp.webextension.testing.WalletDomSpec
import org.scalatest.Assertion
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.must.Matchers._
import org.scalatest.wordspec.AsyncWordSpec
import scalatags.JsDom.all.div
import typings.std.{HTMLElement, HTMLLabelElement, document}
import Wallet._
import scala.concurrent.{ExecutionContext, Future}

class MainWalletViewSpec extends AsyncWordSpec with WalletDomSpec with ScalaFutures {

  override implicit def executionContext =
    org.scalatest.concurrent.TestExecutionContext.runNow

  override def beforeEach(): Unit = {
    super.beforeEach()

  }

  "MainWalletView" should {
    "load Main view with wallet status missing" in {
      withWallet {
        val backgroundAPI = new BackgroundAPI()
        val container = div().render
        document.body.appendChild(container)
        MainWalletView(backgroundAPI).mainWalletScreen(container).map { _ =>
          val statusLabel = document
            .querySelector("._label_update")
            .asInstanceOf[HTMLLabelElement]
          val mainView =
            document.querySelector("#mainView").asInstanceOf[HTMLElement]
          mainView must not be null
          statusLabel.textContent mustBe "Missing"
        }
      }
    }

    "load Main view with wallet status Unlocked" in {
      withWallet {
        val backgroundAPI = new BackgroundAPI()
        setUpWallet {
          backgroundAPI.createWallet(PASSWORD, Mnemonic(), Role.Verifier, ORGANISATION_NAME, Array())
        }.flatMap { _ =>
          val container = div().render
          document.body.appendChild(container)
          MainWalletView(backgroundAPI).mainWalletScreen(container).map { _ =>
            val statusLabel = document
              .querySelector("._label_update")
              .asInstanceOf[HTMLLabelElement]
            val mainView =
              document.querySelector("#mainView").asInstanceOf[HTMLElement]
            mainView must not be null
            statusLabel.textContent mustBe "Unlocked"
          }
        }
      }
    }

    "load Main view with wallet status Locked" in {
      withWallet {
        val backgroundAPI = new BackgroundAPI()
        setUpWallet {
          for {
            _ <- backgroundAPI.createWallet(PASSWORD, Mnemonic(), Role.Verifier, ORGANISATION_NAME, Array())
            _ <- backgroundAPI.lockWallet()
          } yield ()
        }.flatMap { _ =>
          val container = div().render
          document.body.appendChild(container)
          MainWalletView(backgroundAPI).mainWalletScreen(container).map { _ =>
            val statusLabel = document
              .querySelector("._label_update")
              .asInstanceOf[HTMLLabelElement]
            val mainView =
              document.querySelector("#mainView").asInstanceOf[HTMLElement]
            mainView must not be null
            statusLabel.textContent mustBe "Locked"
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
