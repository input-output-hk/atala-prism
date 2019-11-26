package io.iohk.cvp.wallet

import org.scalatest.{BeforeAndAfterEach, MustMatchers, OptionValues, WordSpec}
import org.scalatest.concurrent.ScalaFutures
import os.Path

import scala.concurrent.ExecutionContext.Implicits.global

class WalletIOSpec extends WordSpec with ScalaFutures with MustMatchers with BeforeAndAfterEach with OptionValues {

  val filePath: Path = os.pwd / ".test" / "test-wallet.dat"

  override def afterEach(): Unit = {
    super.afterEach()
    if (os.exists(filePath)) os.remove(filePath)
  }

  "save wallet" should {
    "store file" in {
      val walletIO = WalletIO(filePath)
      val wallet = "saved-wallet".getBytes
      val responseF = walletIO.save(wallet)
      responseF.futureValue mustBe ()
    }
  }

  "Load wallet" should {
    "load file from disk" in {
      val wallet = "saved-wallet".getBytes
      val walletIO = WalletIO(filePath)
      walletIO.save(wallet).futureValue
      val responseF = walletIO.load()
      responseF.futureValue.value mustBe wallet
    }

    "return None if file doesn't exists" in {
      val walletIO = WalletIO(filePath)
      val responseF = walletIO.load()
      responseF.futureValue mustBe None
    }
  }

}
