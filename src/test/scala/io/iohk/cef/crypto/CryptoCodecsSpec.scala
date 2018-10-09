package io.iohk.cef.crypto

import org.scalatest.FlatSpec
import org.scalatest.Matchers._

class CryptoCodecsSpec extends FlatSpec {

  behavior of "EncodingHelpers"

  they should "support encoding/decoding of Signature" in {
    val signingKeyPair = generateSigningKeyPair()

    val signature = sign("foo", signingKeyPair.`private`)

    NioDecoder[Signature].decode(NioEncoder[Signature].encode(signature)) shouldBe Some(signature)
  }

  they should "support encoding/decoding of SigningPublicKey" in {
    val signingPublicKey = generateSigningKeyPair().public

    NioDecoder[SigningPublicKey].decode(NioEncoder[SigningPublicKey].encode(signingPublicKey)) shouldBe Some(
      signingPublicKey)
  }
}
