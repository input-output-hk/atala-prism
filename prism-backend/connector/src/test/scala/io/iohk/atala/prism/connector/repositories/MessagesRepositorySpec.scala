package io.iohk.atala.prism.connector.repositories

import cats.data.NonEmptyList
import com.softwaremill.diffx.scalatest.DiffMatcher._
import doobie.Fragments
import doobie.implicits._
import io.iohk.atala.prism.connector.model.{ConnectionId, ConnectionStatus}
import io.iohk.atala.prism.connector.repositories.daos._
import io.iohk.atala.prism.models.ParticipantId
import io.iohk.atala.prism.protos.connector_models.MessageToSendByConnectionToken
import io.iohk.atala.prism.protos.credential_models
import io.iohk.atala.prism.repositories.ops.SqlTestOps.Implicits
import org.scalatest.OptionValues._

import java.time.{Instant, LocalDateTime, ZoneOffset}

class MessagesRepositorySpec extends ConnectorRepositorySpecBase {
  lazy val messagesRepository = new MessagesRepository(database)

  "insertMessage" should {
    "insert message from the initiator to the acceptor" in {
      val issuer = createIssuer()
      val holder = createHolder()
      val connection = createConnection(issuer, holder)
      val message = "hello".getBytes

      val result = messagesRepository.insertMessage(issuer, connection, message).value.futureValue
      val messageId = result.toOption.value

      val (sender, recipient, content) =
        sql"""
             |SELECT sender, recipient, content
             |FROM messages
             |WHERE id = $messageId
           """.stripMargin.runUnique[(ParticipantId, ParticipantId, Array[Byte])]()

      sender mustBe issuer
      recipient mustBe holder
      content mustBe message
    }

    "insert message from the acceptor to the initiator" in {
      val issuer = createIssuer()
      val holder = createHolder()
      val connection = createConnection(issuer, holder)
      val message = "hello".getBytes

      val result = messagesRepository.insertMessage(holder, connection, message).value.futureValue
      val messageId = result.toOption.value

      val (sender, recipient, content) =
        sql"""
             |SELECT sender, recipient, content
             |FROM messages
             |WHERE id = $messageId
           """.stripMargin.runUnique[(ParticipantId, ParticipantId, Array[Byte])]()

      sender mustBe holder
      recipient mustBe issuer
      content mustBe message
    }

    "fail when the sender has no other side" in {
      val issuer = createIssuer()
      val connection = ConnectionId.random()
      val message = "hello".getBytes

      val caught = intercept[RuntimeException] {
        messagesRepository.insertMessage(issuer, connection, message).value.futureValue
      }
      caught.getCause must not be null
      caught.getCause.getMessage mustBe s"Failed to send message, the connection $connection with sender $issuer doesn't exist"
    }
  }

  "insert many messages from the initiator to the acceptors" in {
    val issuer = createIssuer()
    val holder1 = createHolder()
    val holder2 = createHolder()
    val token1 = createToken(issuer)
    val token2 = createToken(issuer)
    createConnection(issuer, holder1, token1, ConnectionStatus.InvitationMissing)
    createConnection(issuer, holder2, token2, ConnectionStatus.InvitationMissing)

    val message = credential_models.AtalaMessage()

    val messages =
      NonEmptyList.of(
        MessageToSendByConnectionToken(token1.token, Some(message)),
        MessageToSendByConnectionToken(token2.token, Some(message))
      )

    val result = messagesRepository.insertMessages(issuer, messages).value.futureValue
    val messagesIds = NonEmptyList.fromList(result.toOption.value).get

    val insertedMessages =
      (fr"""
           |SELECT sender, recipient, content
           |FROM messages
           |WHERE """.stripMargin ++ Fragments.in(fr"id", messagesIds))
        .query[(ParticipantId, ParticipantId, Array[Byte])]
        .to[List]
        .transact(database)
        .unsafeToFuture()
        .futureValue

    insertedMessages.foreach {
      case (messageSender, messageRecipient, messageContent) =>
        messageSender mustBe issuer
        List(holder1, holder2) must contain(messageRecipient)
        messageContent mustBe message.toByteArray
    }
  }

  "insert many messages from the acceptor to the initiator" in {
    val issuer = createIssuer()
    val holder = createHolder()
    val token = createToken(issuer)
    createConnection(issuer, holder, token, ConnectionStatus.InvitationMissing)
    createConnection(issuer, holder, token, ConnectionStatus.InvitationMissing)

    val message1 =
      credential_models.AtalaMessage().withProofRequest(credential_models.ProofRequest(connectionToken = "token1"))

    val message2 =
      credential_models.AtalaMessage().withProofRequest(credential_models.ProofRequest(connectionToken = "token2"))

    val messages =
      NonEmptyList.of(
        MessageToSendByConnectionToken(token.token, Some(message1)),
        MessageToSendByConnectionToken(token.token, Some(message2))
      )

    val result = messagesRepository.insertMessages(holder, messages).value.futureValue
    val messagesIds = NonEmptyList.fromList(result.toOption.value).get

    val insertedMessages =
      (fr"""
           |SELECT sender, recipient, content
           |FROM messages
           |WHERE """.stripMargin ++ Fragments.in(fr"id", messagesIds))
        .query[(ParticipantId, ParticipantId, Array[Byte])]
        .to[List]
        .transact(database)
        .unsafeToFuture()
        .futureValue

    insertedMessages.foreach {
      case (messageSender, messageRecipient, messageContent) =>
        messageSender mustBe holder
        messageRecipient mustBe issuer
        List(message1.toByteArray, message2.toByteArray) must contain(messageContent)
    }
  }

  "fail to insert many messages if one of the connection token is invalid" in {
    val issuer = createIssuer()
    val holder1 = createHolder()
    val holder2 = createHolder()
    val token1 = createToken(issuer)
    val token2 = createToken(issuer)
    createConnection(issuer, holder1, token1, ConnectionStatus.InvitationMissing)
    createConnection(issuer, holder2, token2, ConnectionStatus.InvitationMissing)

    val message = credential_models.AtalaMessage()

    val messages =
      NonEmptyList.of(
        MessageToSendByConnectionToken(token1.token, Some(message)),
        MessageToSendByConnectionToken("invalidConnectionToken", Some(message))
      )

    val result = messagesRepository.insertMessages(issuer, messages).value.futureValue
    result mustBe a[Left[_, _]]
  }

  "getMessagesPaginated" should {
    "select subset of messages according to since and limit" in {
      val issuer = createIssuer()
      val holder = createHolder()
      val connection = createConnection(issuer, holder)
      val zeroTime = LocalDateTime.of(2019, 10, 10, 12, 14, 17, 5000).toEpochSecond(ZoneOffset.UTC)

      for (i <- 0 to 20) yield {
        val messageId =
          createMessage(connection, issuer, holder, Instant.ofEpochMilli(zeroTime + i), s"hello$i".getBytes())
        i -> messageId
      }

      val all = messagesRepository
        .getMessagesPaginated(holder, 20, Option.empty)
        .value
        .futureValue
        .toOption
        .value
        .map(_.id)

      val firstTenExpected = all.take(10)
      val nextTenExpected = all.slice(10, 20)

      val firstTenResult = messagesRepository.getMessagesPaginated(holder, 10, Option.empty).value.futureValue
      firstTenResult.toOption.value.map(_.id) must matchTo(firstTenExpected)

      val nextTenResult =
        messagesRepository.getMessagesPaginated(holder, 10, Some(firstTenExpected.last)).value.futureValue
      nextTenResult.toOption.value.map(_.id) must matchTo(nextTenExpected)
    }
  }
}
