package io.iohk.cvp.wallet

import io.iohk.cvp.wallet.protos.WalletData
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Second, Seconds, Span}
import org.scalatest.{BeforeAndAfterEach, MustMatchers, OptionValues, WordSpec}
import os.Path

import scala.concurrent._
import ExecutionContext.Implicits.global
import scala.concurrent.duration._
class WalletServiceOrchestratorSpec
    extends WordSpec
    with ScalaFutures
    with MustMatchers
    with BeforeAndAfterEach
    with OptionValues {

  implicit val pc: PatienceConfig = PatienceConfig(10.seconds, 10.millis)

  private val filePath: Path = os.pwd / ".test" / "testwallet.dat"
  private val passPhrase = "password"
  private val walletIO = WalletIO(filePath)
  private val walletSecurity = WalletSecurity()

  override def afterEach(): Unit = {
    super.afterEach()
    if (os.exists(filePath)) os.remove(filePath)
  }

  "WalletServiceOrchestrator" should {

    "create new wallet" in {
      val walletServiceOrchestrator = new WalletServiceOrchestrator(walletSecurity, walletIO)
      val walletData = walletServiceOrchestrator.createNewWallet(passPhrase).futureValue
      walletData.did mustBe "did:iohk:test"
    }

    "create new wallet throw Runtime exception if wallet already exits" in {
      val walletServiceOrchestrator = new WalletServiceOrchestrator(walletSecurity, walletIO)
      walletServiceOrchestrator.createNewWallet(passPhrase).futureValue
      intercept[RuntimeException] {
        walletServiceOrchestrator.createNewWallet(passPhrase).futureValue
      }
    }

    "load a existing wallet" in {
      val walletServiceOrchestrator = new WalletServiceOrchestrator(walletSecurity, walletIO)
      val walletData = walletServiceOrchestrator.createNewWallet(passPhrase).futureValue
      val loadedWalletData = walletServiceOrchestrator.loadWallet(passPhrase).futureValue
      loadedWalletData.value mustBe walletData.toByteArray
    }

    "save or update existing wallet" in {
      val walletServiceOrchestrator = new WalletServiceOrchestrator(walletSecurity, walletIO)
      val walletData = walletServiceOrchestrator.createNewWallet(passPhrase).futureValue
      val newPassPhrase = "new password"
      val savedWalletData: Unit = walletServiceOrchestrator.save(newPassPhrase, walletData).futureValue
      savedWalletData mustBe ()
    }
  }

}
