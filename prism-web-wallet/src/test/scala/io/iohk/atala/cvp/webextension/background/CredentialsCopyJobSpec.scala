package io.iohk.atala.cvp.webextension.background

import java.util.UUID

import com.google.protobuf.ByteString
import io.iohk.atala.prism.protos.{connector_models, credential_models}
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.OptionValues._

class CredentialsCopyJobSpec extends AnyWordSpec {
  "buildRequestFromConnectorMessage" should {
    val connectionId = UUID.randomUUID().toString
    val messageId = UUID.randomUUID().toString

    "correctly process PlainTextCredentialMessages" in {
      val encodedCredential = "random text"
      val plainTextCredential =
        credential_models
          .AtalaMessage()
          .withPlainCredential(
            credential_models
              .PlainTextCredential()
              .withEncodedCredential(encodedCredential)
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

    "return an error when parsing a non PlainTextCredentialMessage" in {
      val nonPlainTextCredentialMessage =
        credential_models
          .AtalaMessage()
          .withIssuerSentCredential(
            credential_models.IssuerSentCredential()
          )

      val receivedMessage = connector_models
        .ReceivedMessage()
        .withConnectionId(connectionId)
        .withId(messageId)
        .withMessage(ByteString.copyFrom(nonPlainTextCredentialMessage.toByteArray))

      val parsed = CredentialsCopyJob.buildRequestFromConnectorMessage(receivedMessage).toOption

      parsed mustBe None
    }
  }
}
