package io.iohk.cvp.intdemo

import java.time.Instant
import java.util.UUID

import credential.Credential
import io.iohk.connector.model.{ConnectionId, Message, MessageId}
import io.iohk.cvp.intdemo.SharedCredentials.credentialsOfType
import org.scalatest.FlatSpec
import org.scalatest.Matchers._

class SharedCredentialsSpec extends FlatSpec {

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

  private def credential(typeId: String): Credential = {
    Credential(typeId = typeId, credentialDocument = "foo")
  }

  private def credentialMessage(typeId: String): Array[Byte] = {
    credential(typeId).toByteArray
  }
}
