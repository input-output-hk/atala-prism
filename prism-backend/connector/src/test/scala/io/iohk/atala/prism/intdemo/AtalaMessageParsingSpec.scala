package io.iohk.atala.prism.intdemo

import io.iohk.atala.prism.protos.credential_models.AtalaMessage
import io.iohk.atala.prism.protos.credential_models.PlainTextCredential
import io.iohk.atala.prism.protos.credential_models.ProofRequest
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers._

class AtalaMessageParsingSpec extends AnyFlatSpec {

  "Protobuff" should "parse a credential" in {
    val encodedCredential = "some json credential"
    val encodedMerkelProof = "encoded Merkle proof"
    val credential = PlainTextCredential(encodedCredential, encodedMerkelProof)

    val credentialProto =
      AtalaMessage().withPlainCredential(credential).toByteArray

    val parsedCredential: AtalaMessage = AtalaMessage.parseFrom(credentialProto)

    parsedCredential.message.isPlainCredential shouldBe true
    parsedCredential.getPlainCredential shouldBe (credential)

  }

  it should "parse a proof request" in {
    val proofRequest = ProofRequest(Seq("type-id"), "connection-token")
    val proofRequestProto =
      AtalaMessage().withProofRequest(proofRequest).toByteArray

    val parsedProofRequest = AtalaMessage.parseFrom(proofRequestProto)

    parsedProofRequest.message.isProofRequest shouldBe true
    parsedProofRequest.getProofRequest shouldBe proofRequest
  }
}
