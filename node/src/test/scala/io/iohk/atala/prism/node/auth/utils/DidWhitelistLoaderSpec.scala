package io.iohk.atala.prism.node.auth.utils

import com.typesafe.config.ConfigFactory
import io.iohk.atala.prism.node.identity.{PrismDid => DID}
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec

class DidWhitelistLoaderSpec extends AnyWordSpec with Matchers {
  "DidWhitelistLoader" should {
    "be able to load whitelist from file" in {
      val globalConfig = ConfigFactory.load()
      DidWhitelistLoader.load(globalConfig, "nodeExplorer") must be(
        Set(
          DID.fromString(
            "did:prism:5f0ffa312e8c6f260dbe6dbaa1e4e0d685aba03297c4e4f9ae80fa8d3fd7c0b0:Cj8KPRI7CgdtYXN0ZXIwEAFKLgoJc2VjcDI1NmsxEiEDhyJiYbQZs28bivj9PXsitEWca1MDg3yeW9ziiNcG-Cs"
          )
        )
      )
    }
  }
}
