package io.iohk.cvp.wallet

import java.util.UUID

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfterEach, MustMatchers, OptionValues, WordSpec}

class WalletSecuritySpec
    extends WordSpec
    with ScalaFutures
    with MustMatchers
    with BeforeAndAfterEach
    with OptionValues {

  "WalletSecurity" should {
    "encrypt and decrypt" in {
      val keySalt = UUID.randomUUID().toString
      val walletSecurity = WalletSecurity(keySalt)
      val keySpec = walletSecurity.generateSecretKey("testphrase")
      val bytes = "encrypt".getBytes()
      val encryptedBytes = walletSecurity.encrypt(keySpec, bytes)
      val decryptedBytes = walletSecurity.decrypt(keySpec, encryptedBytes)
      bytes mustBe decryptedBytes

    }

  }

}
