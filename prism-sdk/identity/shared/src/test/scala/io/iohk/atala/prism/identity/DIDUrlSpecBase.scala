package io.iohk.atala.prism.identity

import io.iohk.atala.prism.identity.DIDUrl.{EmptyDidScheme, EmptyDidSuffix, InvalidDid, InvalidUrl}
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.EitherValues._
import org.scalatest.OptionValues._

class DIDUrlSpecBase extends AnyWordSpec {
  "DIDUrl.fromString" should {
    val did = DID.buildPrismDID("abacaba")

    "support a plain valid DID" in {
      DIDUrl.fromString(did.value).toOption.value mustBe DIDUrl(did, Vector.empty, Map.empty, None)
    }

    "support paths" in {
      DIDUrl.fromString(s"${did.value}/path1/path2").toOption.value mustBe
        DIDUrl(did, Vector("path1", "path2"), Map.empty, None)
    }

    "support parameters" in {
      DIDUrl.fromString(s"${did.value}?param1=value1&param2=value2").toOption.value mustBe
        DIDUrl(did, Vector.empty, Map("param1" -> Vector("value1"), "param2" -> Vector("value2")), None)
    }

    "support array parameters" in {
      DIDUrl.fromString(s"${did.value}?param1=value1&param1=value2").toOption.value mustBe
        DIDUrl(did, Vector.empty, Map("param1" -> Vector("value1", "value2")), None)
    }

    "support fragments" in {
      DIDUrl.fromString(s"${did.value}#fragment").toOption.value mustBe
        DIDUrl(did, Vector.empty, Map.empty, Some("fragment"))
    }

    "support all at once" in {
      DIDUrl.fromString(s"${did.value}/path?param=value#fragment").toOption.value mustBe
        DIDUrl(did, Vector("path"), Map("param" -> Vector("value")), Some("fragment"))
    }

    "fail on invalid urls" in {
      DIDUrl.fromString("*!@#$%^&*()_+").left.value mustBe InvalidUrl("*!@#$%^&*()_+")
    }

    "fail on empty DID scheme" in {
      DIDUrl.fromString("prism").left.value mustBe EmptyDidScheme
    }

    "fail on emtpy DID suffix" in {
      DIDUrl.fromString("did:").left.value mustBe EmptyDidSuffix
    }

    "fail on invalid DIDs" in {
      DIDUrl.fromString("did:notprism:abcdef").left.value mustBe InvalidDid("did:notprism:abcdef")
    }
  }

  "DIDUrl" should {
    "recognize keyId when present" in {
      DIDUrl.fromString("did:prism:abc/keyId/master0").toOption.value.keyIdOption.value mustBe "master0"
    }

    "not recognize keyId when not present" in {
      DIDUrl.fromString("did:prism:abc/otherId/master0").toOption.value.keyIdOption mustBe None
    }
  }
}
