package io.iohk.atala.prism.node.identity

import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import org.scalatest.wordspec.AnyWordSpec
import io.iohk.atala.prism.node.crypto.CryptoUtils.Sha256Hash
import io.iohk.atala.prism.node.utils.Base64Utils

class PrismDidSpec extends AnyWordSpec {

  "PrismDid library" should {

    val canonicalSuffixHex = "9b5118411248d9663b6ab15128fba8106511230ff654e7514cdcc4ce919bde9b"
    val canonicalSuffix = Sha256Hash.fromHex(canonicalSuffixHex)
    val encodedStateUsedBase64 =
      "Cj8KPRI7CgdtYXN0ZXIwEAFKLgoJc2VjcDI1NmsxEiEDHpf-yhIns-LP3tLvA8icC5FJ1ZlBwbllPtIdNZ3q0jU"
    val encodedStateUsed = Base64Utils.decodeURL(encodedStateUsedBase64)

    val short = PrismDid.buildCanonical(canonicalSuffix)
    val long = PrismDid.buildLongForm(canonicalSuffix, encodedStateUsed)

    // HASHING
    "asCanonical should work for long and short form dids" in {
      canonicalSuffixHex mustBe short.asCanonical().suffix
      canonicalSuffixHex mustBe long.asCanonical().suffix
    }


  }
}
