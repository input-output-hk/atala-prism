package io.iohk.atala.cvp.webextension.background

import io.iohk.atala.cvp.webextension.Config
import io.iohk.atala.cvp.webextension.activetab.ActiveTabConfig
import io.iohk.atala.cvp.webextension.background.wallet.{Role, SigningRequest, WalletStatus}
import io.iohk.atala.cvp.webextension.common.Mnemonic
import io.iohk.atala.cvp.webextension.testing.FakeApis
import org.scalatest.matchers.must.Matchers._
import org.scalatest.wordspec.AsyncWordSpec
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}

import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.scalajs.concurrent.JSExecutionContext
import scala.scalajs.js

class BackgroundAPISpec extends AsyncWordSpec with BeforeAndAfterAll with BeforeAndAfterEach {
  // This test is truly async and needs to override the default serial execution context, read more at
  // https://github.com/scalatest/scalatest/issues/1039
  implicit override def executionContext: ExecutionContextExecutor = JSExecutionContext.Implicits.queue

  val PASSWORD = "test-password"
  val TEST_KEY = "test-key"
  val ORGANISATION_NAME = "IOHK"

  override def beforeAll(): Unit = {
    // Fake the APIs only available in the browser
    FakeApis.configure()
  }

  override def beforeEach(): Unit = {
    // Remove any listener from any previous test
    js.Dynamic.global.chrome.runtime.onMessage.removeAllListeners()

    // Delete any data stored by any previous test
    js.Dynamic.global.chrome.storage.local.clear()

    // Run the background script
    Runner(
      Config(
        ActiveTabConfig()
      )
    ).run()
  }

  def setUpWallet(api: BackgroundAPI, keys: Seq[String] = List()): Future[Unit] = {
    for {
      _ <- api.createWallet(PASSWORD, Mnemonic(), Role.Verifier, ORGANISATION_NAME, Array())
      _ <- Future.sequence(keys.map(api.createKey))
    } yield ()
  }

  "createKey" should {
    "fail when the wallet is not loaded" in {
      val api = new BackgroundAPI()

      recoverToExceptionIf[RuntimeException] {
        for {
          _ <- api.createKey(TEST_KEY)
        } yield ()
      }.map(_.getMessage mustBe "The wallet has not been loaded")
    }

    "create a new key" in {
      val api = new BackgroundAPI()

      for {
        _ <- setUpWallet(api)

        _ <- api.createKey(TEST_KEY)
        keys <- api.listKeys()
      } yield {
        keys.names mustBe Array(TEST_KEY)
      }
    }

    "fail when creating an existing key" in {
      val api = new BackgroundAPI()

      recoverToExceptionIf[RuntimeException] {
        for {
          _ <- setUpWallet(api, List(TEST_KEY))

          _ <- api.createKey(TEST_KEY)
        } yield ()
      }.map(_.getMessage mustBe "Key exists")
    }
  }

  "listKeys" should {
    "return no keys when the wallet is not loaded" in {
      val api = new BackgroundAPI()

      for {
        keys <- api.listKeys()
      } yield {
        keys.names mustBe empty
      }
    }

    "return all keys" in {
      val api = new BackgroundAPI()

      for {
        _ <- setUpWallet(api, List(TEST_KEY))

        keys <- api.listKeys()
      } yield {
        keys.names mustBe Array(TEST_KEY)
      }
    }
  }

  "requestSignature" should {
    "get the message signed" in {
      val api = new BackgroundAPI()

      for {
        _ <- setUpWallet(api, List(TEST_KEY))

        // Fire request, but wait its result until signing happens
        signatureResultFuture = api.requestSignature("sign-me")
        _ <- api.signRequestWithKey(1, TEST_KEY)
        signatureResult <- signatureResultFuture
      } yield {
        // signature is encoded so it's pointless to test its actual value
        signatureResult.signature must not be empty
      }
    }

    "be able to request signature without the wallet loaded" in {
      val api = new BackgroundAPI()

      // Fire request, but wait its result until signing happens
      val signatureResultFuture = api.requestSignature("sign-me")
      for {
        _ <- setUpWallet(api, List(TEST_KEY))
        _ <- api.signRequestWithKey(1, TEST_KEY)
        signatureResult <- signatureResultFuture
      } yield {
        // signature is encoded so it's pointless to test its actual value
        signatureResult.signature must not be empty
      }
    }
  }

  "getSignatureRequests" should {
    "return the pending signature requests" in {
      val api = new BackgroundAPI()

      for {
        _ <- setUpWallet(api)
        _ = api.requestSignature("sign-me")

        signingRequests <- api.getSignatureRequests()
      } yield {
        signingRequests.requests mustBe List(SigningRequest(1, "sign-me"))
      }
    }

    "return the pending signature requests even with the wallet not loaded" in {
      val api = new BackgroundAPI()

      api.requestSignature("sign-me")
      for {
        signingRequests <- api.getSignatureRequests()
      } yield {
        signingRequests.requests mustBe List(SigningRequest(1, "sign-me"))
      }
    }
  }

  "signRequestWithKey" should {
    "sign the pending request" in {
      val api = new BackgroundAPI()

      for {
        _ <- setUpWallet(api, List(TEST_KEY))
        // Do not wait on the request
        _ = api.requestSignature("sign-me")

        _ <- api.signRequestWithKey(1, TEST_KEY)
        signatureRequests <- api.getSignatureRequests()
      } yield {
        signatureRequests.requests mustBe empty
      }
    }

    "fail when requestId does not exist" in {
      val api = new BackgroundAPI()

      recoverToExceptionIf[RuntimeException] {
        for {
          _ <- setUpWallet(api, List(TEST_KEY))

          _ <- api.signRequestWithKey(1, TEST_KEY)
        } yield ()
      }.map(_.getMessage mustBe "Unknown request")
    }

    "fail when key does not exist" in {
      val api = new BackgroundAPI()

      recoverToExceptionIf[RuntimeException] {
        for {
          _ <- setUpWallet(api)
          _ = api.requestSignature("sign-me")

          _ <- api.signRequestWithKey(1, TEST_KEY)
        } yield ()
      }.map(_.getMessage mustBe "Unknown key test-key")
    }

    "fail when wallet is not loaded" in {
      val api = new BackgroundAPI()

      api.requestSignature("sign-me")
      recoverToExceptionIf[RuntimeException] {
        for {
          _ <- api.signRequestWithKey(1, TEST_KEY)
        } yield ()
      }.map(_.getMessage mustBe "Unknown key test-key")
    }
  }

  "getWalletStatus" should {
    "return Missing when no wallet has been created" in {
      val api = new BackgroundAPI()

      for {
        status <- api.getWalletStatus()
      } yield {
        status.status mustBe WalletStatus.Missing
      }
    }

    "return Locked when wallet has been set but not loaded" in {
      val api = new BackgroundAPI()

      for {
        _ <- setUpWallet(api)
        _ <- api.lockWallet()

        status <- api.getWalletStatus()
      } yield {
        status.status mustBe WalletStatus.Locked
      }
    }

    "return Unlocked when wallet has been loaded" in {
      val api = new BackgroundAPI()

      for {
        _ <- setUpWallet(api)

        status <- api.getWalletStatus()
      } yield {
        status.status mustBe WalletStatus.Unlocked
      }
    }
  }

  "createWallet" should {
    "create the wallet" in {
      val api = new BackgroundAPI()

      for {
        _ <- api.createWallet(PASSWORD, Mnemonic(), Role.Verifier, ORGANISATION_NAME, Array())
        status <- api.getWalletStatus()
      } yield {
        status.status mustBe WalletStatus.Unlocked
      }
    }

    "create the wallet again even if already created" in {
      val api = new BackgroundAPI()

      for {
        _ <- api.createWallet(PASSWORD, Mnemonic(), Role.Verifier, ORGANISATION_NAME, Array())

        _ <- api.createWallet(PASSWORD, Mnemonic(), Role.Verifier, ORGANISATION_NAME, Array())
        status <- api.getWalletStatus()
      } yield {
        status.status mustBe WalletStatus.Unlocked
      }
    }
  }

  "unlockWallet" should {
    "unlock the wallet" in {
      val api = new BackgroundAPI()

      for {
        _ <- setUpWallet(api)
        _ <- api.lockWallet()

        _ <- api.unlockWallet(PASSWORD)
        status <- api.getWalletStatus()
      } yield {
        status.status mustBe WalletStatus.Unlocked
      }
    }

    "unlock the wallet again when already unlocked" in {
      val api = new BackgroundAPI()

      for {
        _ <- setUpWallet(api)

        _ <- api.unlockWallet(PASSWORD)
        status <- api.getWalletStatus()
      } yield {
        status.status mustBe WalletStatus.Unlocked
      }
    }
  }

  "lockWallet" should {
    "lock the wallet" in {
      val api = new BackgroundAPI()

      for {
        _ <- setUpWallet(api)

        _ <- api.lockWallet()
        status <- api.getWalletStatus()
      } yield {
        status.status mustBe WalletStatus.Locked
      }
    }
  }
}
