package io.iohk.atala.mirror.services

import io.iohk.atala.mirror.MirrorFixtures
import io.iohk.atala.prism.utils.OsUtils
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec

/**
  * This is a test with the real binary for cardano-address, which can be run only on Linux.
  */
class CardanoAddressServiceSpec extends AnyWordSpec with Matchers with MirrorFixtures {

  private val service = new CardanoAddressServiceImpl("../target/mirror-binaries/cardano-address")

  "CardanoAddressService" should {
    "generate address key based on extended public key" in {
      assume(OsUtils.isLinux)

      val path = s"0/${CardanoAddressFixtures.cardanoAddressNo}"
      val generateAddressKeyResult = service.generateAddressKey(CardanoAddressFixtures.acctExtendedVkey, path)
      generateAddressKeyResult mustBe Right(CardanoAddressFixtures.cardanoAddressKey)
    }

    "generate wallet address based on account extended public key and index" in {
      assume(OsUtils.isLinux)

      val generateAddressResult = service.generateWalletAddress(
        CardanoAddressFixtures.acctExtendedVkey,
        CardanoAddressFixtures.cardanoAddressNo,
        "mainnet"
      )
      generateAddressResult mustBe Right(CardanoAddressFixtures.cardanoAddress)
    }
  }

}
