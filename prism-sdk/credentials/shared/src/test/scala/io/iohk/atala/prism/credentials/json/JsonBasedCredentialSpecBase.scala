package io.iohk.atala.prism.credentials.json

import io.circe.Decoder

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.EitherValues

import io.iohk.atala.prism.crypto.{ECTrait, ECSignature}

import io.iohk.atala.prism.credentials.errors.CredentialParsingError
import io.iohk.atala.prism.credentials.CredentialContent

import io.iohk.atala.prism.credentials.json.implicits._

abstract class JsonBasedCredentialSpecBase(implicit ec: ECTrait) extends AnyWordSpec with Matchers with EitherValues {

  "JsonBasedCredential" should {
    "reconstruct the original credential form signed string" in new Fixtures {
      JsonBasedCredential.fromString[CredentialContent[Nothing]](signedCredentialString) mustBe Right(
        JsonBasedCredential(
          contentBytes = emptyCredentialBytes,
          content = emptyCredentialContent,
          signature = Some(ECSignature("signature".getBytes))
        )
      )
    }

    "reconstruct the original credential form signed string to custom subject type" in new Fixtures {
      JsonBasedCredential
        .fromString[CredentialContent[CustomCredentialSubject]](signedCredentialString) mustBe Right(
        JsonBasedCredential(
          contentBytes = emptyCredentialBytes,
          content = emptyCredentialContent,
          signature = Some(ECSignature("signature".getBytes))
        )
      )
    }

    "reconstruct the original credential form unsigned string" in new Fixtures {
      JsonBasedCredential.fromString[CredentialContent[Nothing]](emptyCredential) mustBe Right(
        JsonBasedCredential(
          contentBytes = emptyCredentialBytes,
          content = emptyCredentialContent,
          signature = None
        )
      )
    }

    "allow to parse custom credential subject" in new Fixtures {
      val credential =
        JsonBasedCredential.unsafeFromString[CredentialContent[CustomCredentialSubject]](customCredential)

      credential.content.credentialSubject mustBe
        Some(
          CustomCredentialSubject(id = 1)
        )
    }

    "allow to use custom credential content" in new Fixtures {
      case class CustomCredentialContent(view: String)
      implicit val decodeCustomCredentialContent: Decoder[CustomCredentialContent] =
        Decoder.forProduct1("view")(CustomCredentialContent.apply)
      val credentialString = """{ "view": "html" }"""

      JsonBasedCredential.unsafeFromString[CustomCredentialContent](credentialString) mustBe JsonBasedCredential(
        contentBytes = credentialString.getBytes.toIndexedSeq,
        content = CustomCredentialContent("html"),
        signature = None
      )
    }

    "fail to construct when bytes are not from a valid JSON" in new Fixtures {
      JsonBasedCredential.fromString[CredentialContent[Nothing]]("invalid").left.value mustBe a[CredentialParsingError]
    }

    "sign credential" in new Fixtures {
      val unsignedCredential = JsonBasedCredential(
        contentBytes = emptyCredentialBytes,
        content = emptyCredentialContent,
        signature = None
      )

      unsignedCredential.sign(keys.privateKey).isValidSignature(keys.publicKey) mustBe true
    }

    "compute canonical form" in new Fixtures {
      val unsignedCredential = JsonBasedCredential(
        contentBytes = emptyCredentialBytes,
        content = emptyCredentialContent,
        signature = None
      )
      val signedCredential = unsignedCredential.sign(keys.privateKey)

      unsignedCredential.canonicalForm mustBe emptyCredential
      signedCredential.canonicalForm must startWith("e30=.") // the signature is dynamic
    }

    "be possible to create credential from content" in new Fixtures {
      JsonBasedCredential.fromCredentialContent(emptyCredentialContent) mustBe JsonBasedCredential(
        contentBytes = emptyCredentialBytes,
        content = emptyCredentialContent,
        signature = None
      )
    }
  }

  trait Fixtures {
    lazy val keys = ec.generateKeyPair()

    lazy val emptyCredential = "{}"
    lazy val emptyCredentialBytes = emptyCredential.getBytes.toIndexedSeq // e30=
    lazy val emptyCredentialContent = CredentialContent(Nil, None, None, None, None, None)

    lazy val signedCredentialString = "e30=.c2lnbmF0dXJl" // {}.signature

    val customCredential = """{ "credentialSubject": { "id": 1 } }"""
    case class CustomCredentialSubject(id: Int)

    implicit val decodeCustomCredentialSubject: Decoder[CustomCredentialSubject] =
      Decoder.forProduct1("id")(CustomCredentialSubject.apply)
  }
}
