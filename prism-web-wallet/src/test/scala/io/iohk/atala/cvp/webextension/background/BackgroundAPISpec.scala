package io.iohk.atala.cvp.webextension.background

import io.iohk.atala.cvp.webextension.common.Mnemonic
import io.iohk.atala.cvp.webextension.common.models.Role.Verifier
import io.iohk.atala.cvp.webextension.common.models.{CredentialSubject, PendingRequest, WalletStatus}
import io.iohk.atala.cvp.webextension.testing.WalletDomSpec
import org.scalatest.matchers.must.Matchers._
import org.scalatest.wordspec.AsyncWordSpec

import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.scalajs.concurrent.JSExecutionContext

class BackgroundAPISpec extends AsyncWordSpec with WalletDomSpec {
  // This test is truly async and needs to override the default serial execution context, read more at
  // https://github.com/scalatest/scalatest/issues/1039
  implicit override def executionContext: ExecutionContextExecutor = JSExecutionContext.Implicits.queue

  val PASSWORD = "test-password"
  val TEST_KEY = "test-key"
  val ORGANISATION_NAME = "IOHK"

  def setUpWallet(api: BackgroundAPI, keys: Seq[String] = List()): Future[Unit] = {
    for {
      _ <- api.createWallet(PASSWORD, Mnemonic(), Verifier, ORGANISATION_NAME, Array())
    } yield ()
  }

  "Login request" should {
    "successful create a session" in {
      val api = new BackgroundAPI()
      for {
        _ <- setUpWallet(api, List(TEST_KEY))
        userDetails <- api.login()
      } yield {
        userDetails.name mustBe "IOHK"
      }
    }
  }

  "enqueueRequestApproval" should {
    "create a successful IssueCredential request" in {
      val api = new BackgroundAPI()
      val subject = CredentialSubject("id", Map("key" -> "value"))
      for {
        _ <- setUpWallet(api, List(TEST_KEY))
        u <- api.login()
        res <- api.enqueueRequestApproval(u.sessionId, PendingRequest.IssueCredential(subject))
      } yield {
        res mustBe ()
      }
    }
  }

  "getSignatureRequests" should {
    "return the pending signature requests" in {
      val api = new BackgroundAPI()
      val subject = CredentialSubject("id", Map("key" -> "value"))
      for {
        _ <- setUpWallet(api)
        u <- api.login()
        _ <- api.enqueueRequestApproval(u.sessionId, PendingRequest.IssueCredential(subject))
        signingRequests <- api.getSignatureRequests()
      } yield {
        signingRequests.requests mustBe List(PendingRequest.WithId(0, PendingRequest.IssueCredential(subject)))
      }
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
        _ <- api.createWallet(PASSWORD, Mnemonic(), Verifier, ORGANISATION_NAME, Array())
        status <- api.getWalletStatus()
      } yield {
        status.status mustBe WalletStatus.Unlocked
      }
    }

    "create the wallet again even if already created" in {
      val api = new BackgroundAPI()

      for {
        _ <- api.createWallet(PASSWORD, Mnemonic(), Verifier, ORGANISATION_NAME, Array())

        _ <- api.createWallet(PASSWORD, Mnemonic(), Verifier, ORGANISATION_NAME, Array())
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
