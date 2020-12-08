package io.iohk.atala.prism.credentials

import io.iohk.atala.prism.crypto.ECTrait
import io.iohk.atala.prism.identity.DID
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import org.scalatest.wordspec.AnyWordSpec
import io.iohk.atala.prism.credentials.content.CredentialContent
import io.iohk.atala.prism.credentials.content.syntax._

abstract class CredentialBatchesSpecBase(implicit ec: ECTrait) extends AnyWordSpec {
  private val keys1 = ec.generateKeyPair()
  private val keys2 = ec.generateKeyPair()
  private val keys3 = ec.generateKeyPair()

  private val unsignedCredential = Credential.fromCredentialContent(
    CredentialContent(
      "issuerDid" -> DID.buildPrismDID("123456678abcdefg").value,
      "issuanceKeyId" -> "Issuance-0"
    )
  )

  private val signedCredential1 = unsignedCredential.sign(keys1.privateKey)
  private val signedCredential2 = unsignedCredential.sign(keys2.privateKey)
  private val signedCredential3 = unsignedCredential.sign(keys3.privateKey)

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
