package io.iohk.atala.prism.intdemo

import io.circe.{Json, Printer}
import java.time.Instant
import io.iohk.atala.prism.connector.model.{ConnectionId, Message, MessageId}
import io.iohk.atala.prism.intdemo.SharedCredentials
import io.iohk.atala.prism.models.ParticipantId
import io.iohk.atala.prism.protos.credential_models
import io.iohk.atala.prism.protos.credential_models.AtalaMessage
import io.iohk.atala.prism.utils.Base64Utils
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers._
import org.mockito.MockitoSugar.{when, withObjectMocked}
import org.mockito.ArgumentMatchersSugar.any

class SharedCredentialsSpec extends AnyFlatSpec {

  "getTypeId" should "return the type id of the credential" in {
    val expectedType = "some type"
    val jsonPrinter = Printer(dropNullValues = false, indent = "  ")
    val credentialJson = Json.obj(
      "credentialSubject" -> Json.obj(
        "credentialType" -> Json.fromString(expectedType)
      )
    )
    val credentialDocument = credentialJson.printWith(jsonPrinter)
    val cred = credential(
      encodedCredential = Base64Utils.encodeURL(credentialDocument.getBytes),
      encodedMerkleProof = ""
    )
    val returnedType = SharedCredentials.getTypeId(cred)
    returnedType shouldBe expectedType
  }

  it should "fail if credential document does not follow expected schema" in {
    val jsonPrinter = Printer(dropNullValues = false, indent = "  ")
    val credentialJson = Json.obj(
      "a" -> Json.obj(
        "b" -> Json.fromString("c")
      )
    )
    val credentialDocument = credentialJson.printWith(jsonPrinter)
    val cred = credential(
      encodedCredential = Base64Utils.encodeURL(credentialDocument.getBytes),
      encodedMerkleProof = ""
    )
    intercept[IllegalStateException] {
      SharedCredentials.getTypeId(cred)
    }
  }

  "credentialsOfType" should "ignore invalid messages" in {

    withObjectMocked[SharedCredentials.type] {
      when(
        SharedCredentials.getTypeId(any[credential_models.PlainTextCredential])
      ) thenReturn "some type id!"
      when(
        SharedCredentials.credentialsOfType(any[Set[String]])(any[Seq[Message]])
      ).thenCallRealMethod()

      val message = Message(
        MessageId.random(),
        ConnectionId.random(),
        ParticipantId.random(),
        Instant.now(),
        Array[Byte]()
      )

      SharedCredentials.credentialsOfType(Set("foo"))(
        Seq(message)
      ) shouldBe Seq()
    }

  }

  it should "ignore messages of the wrong type" in {

    withObjectMocked[SharedCredentials.type] {
      val m1 = Message(
        MessageId.random(),
        ConnectionId.random(),
        ParticipantId.random(),
        Instant.now(),
        Array[Byte]()
      )
      val m2 = Message(
        MessageId.random(),
        ConnectionId.random(),
        ParticipantId.random(),
        Instant.now(),
        credentialMessage("A", "A-proof")
      )
      val m3 = Message(
        MessageId.random(),
        ConnectionId.random(),
        ParticipantId.random(),
        Instant.now(),
        credentialMessage("B", "B-proof")
      )
      when(
        SharedCredentials.getTypeId(credential("A", "A-proof"))
      ) thenReturn "A"
      when(
        SharedCredentials.getTypeId(credential("B", "B-proof"))
      ) thenReturn "B"

      when(
        SharedCredentials.credentialsOfType(any[Set[String]])(any[Seq[Message]])
      ).thenCallRealMethod()

      SharedCredentials.credentialsOfType(Set("A"))(
        Seq(m1, m2, m3)
      ) shouldBe Seq(credential("A", "A-proof"))

    }

  }

  private def credential(
      encodedCredential: String,
      encodedMerkleProof: String
  ): credential_models.PlainTextCredential = {
    credential_models.PlainTextCredential(encodedCredential, encodedMerkleProof)
  }

  private def credentialMessage(
      encodedCredential: String,
      encodedMerkleProof: String
  ): Array[Byte] = {
    AtalaMessage()
      .withPlainCredential(credential(encodedCredential, encodedMerkleProof))
      .toByteArray
  }
}
