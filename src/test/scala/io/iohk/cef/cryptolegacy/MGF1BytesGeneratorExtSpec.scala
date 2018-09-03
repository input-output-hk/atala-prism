package io.iohk.cef.cryptolegacy

import akka.util.ByteString
import org.bouncycastle.crypto.Digest
import org.bouncycastle.crypto.digests.{SHA1Digest, SHA256Digest, ShortenedDigest}
import org.bouncycastle.util.encoders.Hex
import org.scalatest.Matchers._
import org.scalatest.{Assertion, FlatSpec}

class MGF1BytesGeneratorExtSpec extends FlatSpec {

  private val seed1 = Hex.decode("d6e168c5f256a2dcff7ef12facd390f393c7a88d")
  private val mask1 = Hex.decode(
    "df79665bc31dc5a62f70535e52c53015b9d37d412ff3c1193439599e1b628774c50d9ccb78d82c425e4521ee47b8c36a4bcffe8b8112a89312fc04420a39de99223890e74ce10378bc515a212b97b8a6447ba6a8870278f0262727ca041fa1aa9f7b5d1cf7f308232fe861")

  private val seed2 = Hex.decode(
    "032e45326fa859a72ec235acff929b15d1372e30b207255f0611b8f785d764374152e0ac009e509e7ba30cd2f1778e113b64e135cf4e2292c75efe5288edfda4")
  private val mask2 = Hex.decode(
    "0e6a26eb7b956ccb8b3bdc1ca975bc57c3989e8fbad31a224655d800c46954840ff32052cdf0d640562bdfadfa263cfccf3c52b29f2af4a1869959bc77f854cf15bd7a25192985a842dbff8e13efee5b7e7e55bbe4d389647c686a9a9ab3fb889b2d7767d3837eea4e0a2f04b53ca8f50fb31225c1be2d0126c8c7a4753b0807")

  private val seed3 = Hex.decode(
    "032e45326fa859a72ec235acff929b15d1372e30b207255f0611b8f785d764374152e0ac009e509e7ba30cd2f1778e113b64e135cf4e2292c75efe5288edfda4")
  private val mask3 = Hex.decode(
    "10a2403db42a8743cb989de86e668d168cbe604611ac179f819a3d18412e9eb45668f2923c087c12fee0c5a0d2a8aa70185401fbbd99379ec76c663e875a60b4aacb1319fa11c3365a8b79a44669f26fb555c80391847b05eca1cb5cf8c2d531448d33fbaca19f6410ee1fcb260892670e0814c348664f6a7248aaf998a3acc6")

  "MGF1BytesGeneratorExt" should "work in scenario #1" in {
    generatorAssertion(seed1, mask1, new ShortenedDigest(new SHA256Digest, 20))
  }

  "MGF1BytesGeneratorExt" should "work in scenario #2" in {
    generatorAssertion(seed2, mask2, new SHA1Digest)
  }

  "MGF1BytesGeneratorExt" should "work in scenario #3" in {
    generatorAssertion(seed3, mask3, new ShortenedDigest(new SHA256Digest, 20))
  }

  private def generatorAssertion(seed: Array[Byte], result: Array[Byte], digest: Digest): Assertion =
    new MGF1BytesGeneratorExt(digest).generateBytes(result.length, seed) shouldBe ByteString(result)

}
