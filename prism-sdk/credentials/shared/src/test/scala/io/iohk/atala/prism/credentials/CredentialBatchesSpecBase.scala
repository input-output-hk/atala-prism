package io.iohk.atala.prism.credentials

import io.iohk.atala.prism.credentials.json.implicits._
import io.iohk.atala.prism.credentials.json.JsonBasedCredential
import io.iohk.atala.prism.crypto.ECTrait
import io.iohk.atala.prism.identity.DID
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import org.scalatest.wordspec.AnyWordSpec

abstract class CredentialBatchesSpecBase(implicit ec: ECTrait) extends AnyWordSpec {
  private val keys1 = ec.generateKeyPair()
  private val keys2 = ec.generateKeyPair()
  private val keys3 = ec.generateKeyPair()

  private val signedCredential1 =
    JsonBasedCredential
      .fromCredentialContent[CredentialContent[Nothing]](
        CredentialContent[Nothing](
          credentialType = Seq(),
          issuerDid = Some(DID.buildPrismDID("123456678abcdefg")),
          issuanceKeyId = Some("Issuance-0"),
          issuanceDate = None,
          expiryDate = None,
          credentialSubject = None
        )
      )
      .sign(keys1.privateKey)

  private val signedCredential2 =
    JsonBasedCredential
      .fromCredentialContent[CredentialContent[Nothing]](
        CredentialContent[Nothing](
          credentialType = Seq(),
          issuerDid = Some(DID.buildPrismDID("123456678abcdefg")),
          issuanceKeyId = Some("Issuance-0"),
          issuanceDate = None,
          expiryDate = None,
          credentialSubject = None
        )
      )
      .sign(keys2.privateKey)

  private val signedCredential3 =
    JsonBasedCredential
      .fromCredentialContent[CredentialContent[Nothing]](
        CredentialContent[Nothing](
          credentialType = Seq(),
          issuerDid = Some(DID.buildPrismDID("123456678abcdefg")),
          issuanceKeyId = Some("Issuance-0"),
          issuanceDate = None,
          expiryDate = None,
          credentialSubject = None
        )
      )
      .sign(keys3.privateKey)

  private val credentialls = List(signedCredential1, signedCredential2, signedCredential3)

  "CredentialBatches" should {
    "generate consistent proofs batches" in {
      val (root, proofs) = CredentialBatches.batch(credentialls)

      val verification = credentialls zip proofs map {
        case (c, p) =>
          CredentialBatches.verifyInclusion(c, root, p)
      }

      verification.forall(identity) mustBe true
    }
  }
}
