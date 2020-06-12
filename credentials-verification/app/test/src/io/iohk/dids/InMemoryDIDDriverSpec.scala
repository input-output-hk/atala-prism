package io.iohk.dids

import java.net.URI
import java.util.UUID

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class InMemoryDIDDriverSpec extends AnyWordSpec with Matchers {

  val exampleUUID = UUID.fromString("7eb48f53-a4ee-4485-8b29-5f1e899adbdb")
  val exampleURI = new URI(s"did:memory:${exampleUUID.toString}")
  val exampleDocument = Document(exampleURI.toString, List.empty)

  "InMemoryDIDDriver" should {
    "return None when asked about non-existing document" in {
      val driver = new InMemoryDIDDriver

      driver.get(exampleURI) shouldBe None
    }

    "create and return DID document" in {
      val driver = new InMemoryDIDDriver

      driver.put(exampleURI, exampleDocument)
      driver.get(exampleURI) shouldBe Some(exampleDocument)
    }

    "update and return DID document" in {
      val updatedDocument = exampleDocument.copy(publicKey = List(null))
      val driver = new InMemoryDIDDriver

      driver.put(exampleURI, exampleDocument)
      driver.update(exampleURI, updatedDocument)
      driver.get(exampleURI) shouldBe Some(updatedDocument)
    }

    "fail when getting non-memory DID" in {
      val driver = new InMemoryDIDDriver
      an[IllegalArgumentException] should be thrownBy {
        driver.get(new URI("did:other:some-id"))
      }
    }

    "fail when creating non-memory DID" in {
      val driver = new InMemoryDIDDriver
      an[IllegalArgumentException] should be thrownBy {
        driver.put(new URI("did:other:some-id"), exampleDocument)
      }
    }

    "fail when updating non-memory DID" in {
      val driver = new InMemoryDIDDriver
      an[IllegalArgumentException] should be thrownBy {
        driver.update(new URI("did:other:some-id"), exampleDocument)
      }
    }

    "fail when getting when identifier is not valid UUID" in {
      val driver = new InMemoryDIDDriver
      a[MalformedDIDException] should be thrownBy {
        driver.get(new URI("did:memory:boris-johnson"))
      }
    }

    "fail when creating when identifier is not valid UUID" in {
      val driver = new InMemoryDIDDriver
      a[MalformedDIDException] should be thrownBy {
        driver.put(new URI("did:memory:boris-johnson"), exampleDocument)
      }
    }

    "fail when updating when identifier is not valid UUID" in {
      val driver = new InMemoryDIDDriver
      a[MalformedDIDException] should be thrownBy {
        driver.update(new URI("did:memory:boris-johnson"), exampleDocument)
      }
    }
  }
}
