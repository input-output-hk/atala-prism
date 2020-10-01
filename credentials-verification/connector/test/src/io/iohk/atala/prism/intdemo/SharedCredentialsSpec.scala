package io.iohk.atala.prism.intdemo

import java.time.Instant
import java.util.UUID

import io.iohk.atala.prism.connector.model.{ConnectionId, Message, MessageId}
import io.iohk.atala.prism.intdemo.SharedCredentials.credentialsOfType
import io.iohk.prism.protos.credential_models
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers._

class SharedCredentialsSpec extends AnyFlatSpec {

  "credentialsOfType" should "ignore invalid messages" in {
    val message = Message(MessageId(UUID.randomUUID()), ConnectionId(UUID.randomUUID()), Instant.now(), Array[Byte]())

    credentialsOfType(Set("foo"))(Seq(message)) shouldBe Seq()
  }

  it should "ignore messages of the wrong type" in {

    val m1 = Message(MessageId.random(), ConnectionId.random(), Instant.now(), Array[Byte]())
    val m2 = Message(MessageId.random(), ConnectionId.random(), Instant.now(), credentialMessage("A"))
    val m3 = Message(MessageId.random(), ConnectionId.random(), Instant.now(), credentialMessage("B"))

    credentialsOfType(Set("A"))(Seq(m1, m2, m3)) shouldBe Seq(credential("A"))
  }

  private def credential(typeId: String): credential_models.Credential = {
    credential_models.Credential(typeId = typeId, credentialDocument = "foo")
  }

  private def credentialMessage(typeId: String): Array[Byte] = {
    credential(typeId).toByteArray
  }
}
