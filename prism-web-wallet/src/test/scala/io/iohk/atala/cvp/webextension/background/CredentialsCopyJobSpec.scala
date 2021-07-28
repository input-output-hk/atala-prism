package io.iohk.atala.cvp.webextension.background

import java.util.UUID
import com.google.protobuf.ByteString
import io.iohk.atala.prism.protos.{connector_models, credential_models}
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.OptionValues._
import typings.inputOutputHkPrismSdk.mod.io.iohk.atala.prism.kotlin.crypto.{MerkleInclusionProof, SHA256DigestCompanion}
import typings.inputOutputHkPrismSdk.mod.io.iohk.atala.prism.kotlin.extras.toList

import scala.scalajs.js
import scala.scalajs.js.typedarray.byteArray2Int8Array

class CredentialsCopyJobSpec extends AnyWordSpec {
  "buildRequestFromConnectorMessage" should {
    val connectionId = UUID.randomUUID().toString
    val messageId = UUID.randomUUID().toString
    val mockHash = SHA256DigestCompanion.compute(byteArray2Int8Array(messageId.getBytes()))
    val mockMerkleProof = new MerkleInclusionProof(mockHash, 0, toList(js.Array()))

    "correctly process PlainTextCredentialMessages" in {
      val encodedCredential = "random text"
      val plainTextCredential =
        credential_models
          .AtalaMessage()
          .withPlainCredential(
            credential_models
              .PlainTextCredential()
              .withEncodedCredential(encodedCredential)
              .withEncodedMerkleProof(mockMerkleProof.encode())
          )

      val receivedMessage = connector_models
        .ReceivedMessage()
        .withConnectionId(connectionId)
        .withId(messageId)
        .withMessage(ByteString.copyFrom(plainTextCredential.toByteArray))

      val parsed = CredentialsCopyJob.buildRequestFromConnectorMessage(receivedMessage).toOption.value

      parsed.connectionId mustBe connectionId
      parsed.credentialExternalId mustBe messageId
      parsed.encodedSignedCredential mustBe encodedCredential
    }
  }
}
