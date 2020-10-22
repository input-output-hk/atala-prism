package io.iohk.dids

import java.net.URI
import java.util.UUID

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class DelegatingDIDResolverSpec extends AnyWordSpec with Matchers {
  val exampleUUID = UUID.fromString("7eb48f53-a4ee-4485-8b29-5f1e899adbdb")
  val exampleURI = new URI(s"did:memory:${exampleUUID.toString}")
  val exampleDocument = Document(exampleURI.toString, List.empty)

  "DelegatingDIDResolver" should {
    "should delegate resolving to method driver" in {
      val driver = new InMemoryDIDDriver()
      driver.put(exampleURI, exampleDocument)

      val resolver = DelegatingDIDResolver("memory" -> driver)

      resolver.get(exampleURI) shouldBe Some(exampleDocument)
    }

    "should return driver for method" in {
      val driver = new InMemoryDIDDriver()

      val resolver = DelegatingDIDResolver("memory" -> driver)

      resolver.getDriver("memory") shouldBe driver
    }

    "should throw an exception when non-did URI passed" in {
      val resolver = DelegatingDIDResolver()

      a[MalformedDIDException] should be thrownBy {
        resolver.get(new URI("http://iohk.io/"))
      }
    }

    "should throw an exception when method missing" in {
      val resolver = DelegatingDIDResolver()

      a[MalformedDIDException] should be thrownBy {
        resolver.get(new URI("did/foo"))
      }
    }

    "should throw an exception when method identifier missing" in {
      val resolver = DelegatingDIDResolver()

      a[MalformedDIDException] should be thrownBy {
        resolver.get(new URI("did:foo"))
      }
    }

    "should throw an exception when method name contains invalid characters" in {
      val resolver = DelegatingDIDResolver()

      a[MalformedDIDException] should be thrownBy {
        resolver.get(new URI("did:in-valid:foo"))
      }
    }

    "should throw an exception when using unknown method" in {
      val resolver = DelegatingDIDResolver()

      an[UnknownMethodException] should be thrownBy {
        resolver.get(new URI("did:pigeons:wilhelm-tell"))
      }
    }

    "should throw an exception when requesting driver for unknown method" in {
      val resolver = DelegatingDIDResolver()

      an[UnknownMethodException] should be thrownBy {
        resolver.getDriver("pigeons")
      }
    }
  }
}
