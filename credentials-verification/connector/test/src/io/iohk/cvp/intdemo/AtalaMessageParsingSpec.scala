package io.iohk.cvp.intdemo

import io.iohk.prism.protos.credential_models.IssuerSentCredential
import io.iohk.prism.protos.credential_models.{AtalaMessage, Credential, ProofRequest}
import org.scalatest.FlatSpec
import org.scalatest.Matchers._

class AtalaMessageParsingSpec extends FlatSpec {

  "Protobuff" should "parse a credential" in {
    val credential = Credential("type-id", "document")

    val credentialProto = AtalaMessage().withIssuerSentCredential(IssuerSentCredential().withCredential(credential)).toByteArray

    val parsedCredential: AtalaMessage = AtalaMessage.parseFrom(credentialProto)

    parsedCredential.message.isHolderSentCredential shouldBe false
    parsedCredential.message.isProofRequest shouldBe false
    parsedCredential.message.isIssuerSentCredential shouldBe true

    parsedCredential.message.issuerSentCredential.get.value.isCredential shouldBe true
    parsedCredential.message.issuerSentCredential.get.value.isAlphaCredential shouldBe false

    parsedCredential.getIssuerSentCredential.getCredential shouldBe credential
  }

  it should "parse a proof request" in {
    val proofRequest = ProofRequest(Seq("type-id"), "connection-token")
    val proofRequestProto = AtalaMessage().withProofRequest(proofRequest).toByteArray

    val parsedProofRequest = AtalaMessage.parseFrom(proofRequestProto)

    parsedProofRequest.message.isHolderSentCredential shouldBe false
    parsedProofRequest.message.isProofRequest shouldBe true
    parsedProofRequest.message.isIssuerSentCredential shouldBe false

    parsedProofRequest.getProofRequest shouldBe proofRequest
  }
}
