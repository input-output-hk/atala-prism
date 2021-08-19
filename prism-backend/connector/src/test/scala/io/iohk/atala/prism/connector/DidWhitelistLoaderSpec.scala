package io.iohk.atala.prism.connector

import com.typesafe.config.ConfigFactory
import io.iohk.atala.prism.kotlin.identity.DID
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec

class DidWhitelistLoaderSpec extends AnyWordSpec with Matchers {
  "DidWhitelistLoader" should {
    "be able to load whitelist from file" in {
      val globalConfig = ConfigFactory.load()
      DidWhitelistLoader.load(globalConfig) must be(
        Set(
          DID.fromString(
            "did:prism:1e8777cf1e014563b123d6eed984ff35d235f64497e6736b7b9647649b6afe8f:CmIKYBJeCgdtYXN0ZXIwEAFCUQoJc2VjcDI1NmsxEiEAwCb_BYvKwhcOIAWiguHbdBfRgJWVO9EvBgWGHPKn9wYaIQDYr0B_6ZsLlfhdE9Nv8-_sZP-l-u8UeUCSbucNiDrrrg"
          )
        )
      )
    }
  }
}
