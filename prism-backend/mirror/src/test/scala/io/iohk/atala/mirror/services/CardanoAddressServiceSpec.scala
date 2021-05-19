package io.iohk.atala.mirror.services

import io.iohk.atala.mirror.MirrorFixtures
import io.iohk.atala.mirror.models.{CardanoAddress, CardanoAddressKey}
import io.iohk.atala.prism.utils.OsUtils
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec

class CardanoAddressServiceSpec extends AnyWordSpec with Matchers with MirrorFixtures {
  private val service = new CardanoAddressService("../target/mirror-binaries/cardano-address")

  // extended public key was generated as:
  // echo "root_xsk12qaq7ccdh97uhvqxfn8l9f5ph0hcpf38nkv3yfpky8uge58gx9xr7up7havl0k8dgy0u38qzakhvmuwtzat4jnvyrjqyvwe2mpewemn6z0wajkjsj2sh3ykj27xkhwhcvwz32wllx6qtvzy4leyz8up6tgd98zkl" | cardano-address key child 1852H/1815H/0H | cardano-address key public --with-chain-code
  val ACCT_EXTENDED_VKEY =
    "acct_xvk155crk6049ap0477qvjpf5mvxtw5f46uk6k54udc9mz5wcdyyhssexcsk5sgvy05m7mqh3ed3qgs6epyf7hvdfxf6hd54aqm3uwdsewqu6vsvy"

  "CardanoAddressService" should {
    "generate address key based on extended public key" in {
      assume(OsUtils.isLinux)

      val path = "0/0"
      val expectedAddressKey =
        "addr_xvk1ywluxn2exkm7gl0m66nnzdkp3fcttkqca48jz3wkdmjmw4vqvahsmk2ajq22f9dwuqjfupeapplreav68dsuyhsv46fdtagj779lrtsmwt0th"

      val generateAddressKeyResult = service.generateAddressKey(ACCT_EXTENDED_VKEY, path)
      generateAddressKeyResult mustBe Right(CardanoAddressKey(expectedAddressKey))
    }

    "generate wallet address based on account extended public key and index" in {
      assume(OsUtils.isLinux)

      val expectedAddress =
        "addr1qyev5h8thxju452j953er64ffcsg4rksx3vr2pxrnsrg50xxrm8nx4l8qntadvlvdfldg8h3v7q7x6s0xcfg54g5c3qqscwuf6"

      val generateAddressResult = service.generateWalletAddress(ACCT_EXTENDED_VKEY, 0, "mainnet")
      generateAddressResult mustBe Right(CardanoAddress(expectedAddress))
    }
  }

}
