package io.iohk.cvp.wallet

import io.iohk.prism.protos.wallet_models
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfterEach, MustMatchers, OptionValues, WordSpec}
import os.Path

import scala.concurrent.ExecutionContext.Implicits.global
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
  private val defaultLogo = List(10, 20, 30).map(_.toByte).toArray

  override def afterEach(): Unit = {
    super.afterEach()
    if (os.exists(filePath)) os.remove(filePath)
  }

  "WalletServiceOrchestrator" should {

    "create new wallet" in {
      val walletServiceOrchestrator = new WalletServiceOrchestrator(walletSecurity, walletIO)
      val walletData =
        walletServiceOrchestrator.createNewWallet(passPhrase, wallet_models.Role.Issuer, "ORG", defaultLogo).futureValue
      walletData.organisationName mustBe "ORG"
      walletData.role mustBe wallet_models.Role.Issuer

    }

    "create new wallet throw Runtime exception if wallet already exits" in {
      val walletServiceOrchestrator = new WalletServiceOrchestrator(walletSecurity, walletIO)
      walletServiceOrchestrator.createNewWallet(passPhrase, wallet_models.Role.Issuer, "ORG", defaultLogo).futureValue
      intercept[RuntimeException] {
        walletServiceOrchestrator.createNewWallet(passPhrase, wallet_models.Role.Issuer, "ORG", defaultLogo).futureValue
      }
    }

    "load a existing wallet" in {
      val walletServiceOrchestrator = new WalletServiceOrchestrator(walletSecurity, walletIO)
      val walletData =
        walletServiceOrchestrator.createNewWallet(passPhrase, wallet_models.Role.Issuer, "ORG", defaultLogo).futureValue
      val loadedWalletData = walletServiceOrchestrator.loadWallet(passPhrase).futureValue
      loadedWalletData.value mustBe walletData.toByteArray
    }

    "save or update existing wallet" in {
      val walletServiceOrchestrator = new WalletServiceOrchestrator(walletSecurity, walletIO)
      val walletData =
        walletServiceOrchestrator.createNewWallet(passPhrase, wallet_models.Role.Issuer, "ORG", defaultLogo).futureValue
      val newPassPhrase = "new password"
      walletServiceOrchestrator.save(newPassPhrase, walletData).futureValue
    }
  }

}
