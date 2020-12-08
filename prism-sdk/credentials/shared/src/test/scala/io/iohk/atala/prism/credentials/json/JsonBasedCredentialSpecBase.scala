package io.iohk.atala.prism.credentials.json

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.{EitherValues, Inside}

import io.iohk.atala.prism.crypto.{ECTrait, ECSignature}

import io.iohk.atala.prism.credentials.errors.CredentialParsingError
import io.iohk.atala.prism.credentials.content.CredentialContent

abstract class JsonBasedCredentialSpecBase(implicit ec: ECTrait)
    extends AnyWordSpec
    with Matchers
    with EitherValues
    with Inside {

  "JsonBasedCredential" should {
    "reconstruct the original credential form signed string" in new Fixtures {
      JsonBasedCredential.fromString(signedCredentialString) mustBe Right(
        JsonBasedCredential(
          contentBytes = emptyCredentialBytes,
          content = emptyCredentialContent,
          signature = Some(ECSignature("signature".getBytes))
        )
      )
    }

    "allow to parse custom credential subject" in new Fixtures {
      val credential =
        JsonBasedCredential.fromString(customCredential)

      credential.map(_.content.getSubFields("credentialSubject")) mustBe Right(Right(Vector("id" -> 1)))
    }

    "fail to construct when bytes are not from a valid JSON" in new Fixtures {
      JsonBasedCredential.fromString("invalid").left.value mustBe a[CredentialParsingError]
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
    lazy val emptyCredentialContent = CredentialContent.empty

    lazy val signedCredentialString = "e30=.c2lnbmF0dXJl" // {}.signature

    // { "credentialSubject": { "id": 1 } }.signature
    val customCredential = "eyAiY3JlZGVudGlhbFN1YmplY3QiOiB7ICJpZCI6IDEgfSB9.c2lnbmF0dXJl"
  }
}
