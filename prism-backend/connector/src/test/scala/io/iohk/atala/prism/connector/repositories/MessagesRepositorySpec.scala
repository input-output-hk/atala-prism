package io.iohk.atala.prism.connector.repositories

import cats.data.NonEmptyList
import com.softwaremill.diffx.generic.auto._
import com.softwaremill.diffx.scalatest.DiffMatcher._
import doobie.Fragments
import doobie.implicits._
import io.iohk.atala.prism.connector.errors.{
  ConnectionNotFoundByConnectionIdAndSender,
  ConnectorError,
  MessageIdsNotUnique,
  MessagesAlreadyExist
}
import io.iohk.atala.prism.connector.model.actions.SendMessagesRequest
import io.iohk.atala.prism.connector.model.{ConnectionId, ConnectionStatus, MessageId, TokenString}
import io.iohk.atala.prism.connector.repositories.daos._
import io.iohk.atala.prism.logging.TraceId
import io.iohk.atala.prism.models.ParticipantId
import io.iohk.atala.prism.protos.credential_models
import io.iohk.atala.prism.repositories.ops.SqlTestOps.Implicits
import io.iohk.atala.prism.utils.IOUtils._
import org.scalatest.OptionValues._

import java.time.{Instant, LocalDateTime, ZoneOffset}

class MessagesRepositorySpec extends ConnectorRepositorySpecBase {
  lazy val messagesRepository = MessagesRepository.unsafe(dbLiftedToTraceIdIO, connectorRepoSpecLogs)

  "insertMessage" should {
    "insert message from the initiator to the acceptor" in {
      val issuer = createIssuer()
      val holder = createHolder()
      val connection = createConnection(issuer, holder)
      val message = "hello".getBytes

      val result =
        messagesRepository.insertMessage(issuer, connection, message).run(TraceId.generateYOLO).unsafeRunSync()
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

      val result =
        messagesRepository.insertMessage(holder, connection, message).run(TraceId.generateYOLO).unsafeRunSync()
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

      val result =
        messagesRepository.insertMessage(issuer, connection, message).run(TraceId.generateYOLO).unsafeRunSync()

      result mustBe an[Left[ConnectionNotFoundByConnectionIdAndSender, MessageId]]
    }

    "insert message with specified id" in {
      val issuer = createIssuer()
      val holder = createHolder()
      val connection = createConnection(issuer, holder)
      val message = "hello".getBytes
      val messageId = MessageId.random()

      val result =
        messagesRepository
          .insertMessage(issuer, connection, message, Some(messageId))
          .run(TraceId.generateYOLO)
          .unsafeRunSync()
      result.toOption.value mustBe messageId

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
  }

  "fail to insert message with specified id when id already exists" in {
    val issuer = createIssuer()
    val holder = createHolder()
    val connection = createConnection(issuer, holder)
    val message = "hello".getBytes
    val messageId = MessageId.random()

    messagesRepository
      .insertMessage(issuer, connection, message, Some(messageId))
      .run(TraceId.generateYOLO)
      .unsafeToFuture()
      .futureValue mustBe Right(
      messageId
    )
    messagesRepository
      .insertMessage(issuer, connection, message, Some(messageId))
      .run(TraceId.generateYOLO)
      .unsafeToFuture()
      .futureValue mustBe an[Left[MessagesAlreadyExist, MessageId]]
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
        SendMessagesRequest.MessageToSend(token1, message.toByteArray, None),
        SendMessagesRequest.MessageToSend(token2, message.toByteArray, None)
      )

    val result = messagesRepository.insertMessages(issuer, messages).run(TraceId.generateYOLO).unsafeRunSync()
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

    insertedMessages.foreach { case (messageSender, messageRecipient, messageContent) =>
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
        SendMessagesRequest.MessageToSend(token, message1.toByteArray, None),
        SendMessagesRequest.MessageToSend(token, message2.toByteArray, None)
      )

    val result = messagesRepository.insertMessages(holder, messages).run(TraceId.generateYOLO).unsafeRunSync()
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

    insertedMessages.foreach { case (messageSender, messageRecipient, messageContent) =>
      messageSender mustBe holder
      messageRecipient mustBe issuer
      List(message1.toByteArray, message2.toByteArray) must contain(messageContent)
    }
  }

  "insert many messages from the initiator to the acceptors with user provided ids" in {
    val issuer = createIssuer()
    val holder1 = createHolder()
    val holder2 = createHolder()
    val token1 = createToken(issuer)
    val token2 = createToken(issuer)
    createConnection(issuer, holder1, token1, ConnectionStatus.InvitationMissing)
    createConnection(issuer, holder2, token2, ConnectionStatus.InvitationMissing)

    val message = credential_models.AtalaMessage()

    val messageId1 = MessageId.random()
    val messageId2 = MessageId.random()

    val messages =
      NonEmptyList.of(
        SendMessagesRequest.MessageToSend(token1, message.toByteArray, Some(messageId1)),
        SendMessagesRequest.MessageToSend(token2, message.toByteArray, Some(messageId2))
      )

    val result = messagesRepository.insertMessages(issuer, messages).run(TraceId.generateYOLO).unsafeRunSync()
    val messagesIds = NonEmptyList.fromList(result.toOption.value).get

    messagesIds.toList.toSet mustBe Set(messageId1, messageId2)
  }

  "fail to insert many messages if user provided ids contain duplicates" in {
    val issuer = createIssuer()
    val holder1 = createHolder()
    val holder2 = createHolder()
    val token1 = createToken(issuer)
    val token2 = createToken(issuer)
    createConnection(issuer, holder1, token1, ConnectionStatus.InvitationMissing)
    createConnection(issuer, holder2, token2, ConnectionStatus.InvitationMissing)

    val message = credential_models.AtalaMessage()

    val messageId1 = MessageId.random()

    val messages =
      NonEmptyList.of(
        SendMessagesRequest.MessageToSend(token1, message.toByteArray, Some(messageId1)),
        SendMessagesRequest.MessageToSend(token2, message.toByteArray, Some(messageId1))
      )

    val result = messagesRepository.insertMessages(issuer, messages).run(TraceId.generateYOLO).unsafeRunSync()
    result mustBe an[Left[MessageIdsNotUnique, List[MessageId]]]
  }

  "fail to insert many messages if messages with provided ids already exists" in {
    val issuer = createIssuer()
    val holder1 = createHolder()
    val holder2 = createHolder()
    val token1 = createToken(issuer)
    val token2 = createToken(issuer)
    createConnection(issuer, holder1, token1, ConnectionStatus.InvitationMissing)
    createConnection(issuer, holder2, token2, ConnectionStatus.InvitationMissing)

    val message = credential_models.AtalaMessage()

    val messageId1 = MessageId.random()
    val messageId2 = MessageId.random()

    val messages =
      NonEmptyList.of(
        SendMessagesRequest.MessageToSend(token1, message.toByteArray, Some(messageId1)),
        SendMessagesRequest.MessageToSend(token2, message.toByteArray, Some(messageId2))
      )

    messagesRepository
      .insertMessages(issuer, messages)
      .run(TraceId.generateYOLO)
      .unsafeToFuture()
      .futureValue mustBe an[Right[ConnectorError, List[MessageId]]]

    val result = messagesRepository.insertMessages(issuer, messages).run(TraceId.generateYOLO).unsafeRunSync()
    result mustBe an[Left[MessagesAlreadyExist, List[MessageId]]]
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
        SendMessagesRequest.MessageToSend(token1, message.toByteArray, None),
        SendMessagesRequest.MessageToSend(TokenString("invalidConnectionToken"), message.toByteArray, None)
      )

    val result = messagesRepository.insertMessages(issuer, messages).run(TraceId.generateYOLO).unsafeRunSync()
    result mustBe a[Left[_, _]]
  }

  "fail to insert many messages if one of the connection tokens is revoked" in {
    val issuer = createIssuer()
    val holder1 = createHolder()
    val holder2 = createHolder()
    val token1 = createToken(issuer)
    val token2 = createToken(issuer)
    createConnection(issuer, holder1, token1, ConnectionStatus.ConnectionAccepted)
    createConnection(issuer, holder2, token2, ConnectionStatus.ConnectionRevoked)

    val message = credential_models.AtalaMessage()

    val messages =
      NonEmptyList.of(
        SendMessagesRequest.MessageToSend(token1, message.toByteArray, None),
        SendMessagesRequest.MessageToSend(token2, message.toByteArray, None)
      )

    val result = messagesRepository.insertMessages(issuer, messages).run(TraceId.generateYOLO).unsafeRunSync()
    result mustBe a[Left[_, _]]
  }

  "fail to insert one message if a connection token is revoked" in {
    val issuer = createIssuer()
    val holder = createHolder()
    val token = createToken(issuer)
    val connectionId = createConnection(issuer, holder, token, ConnectionStatus.ConnectionRevoked)

    val message = credential_models.AtalaMessage()

    val result = messagesRepository
      .insertMessage(issuer, connectionId, message.toByteArray)
      .run(TraceId.generateYOLO)
      .unsafeRunSync()
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
        .run(TraceId.generateYOLO)
        .unsafeToFuture()
        .futureValue
        .toOption
        .value
        .map(_.id)

      val firstTenExpected = all.take(10)
      val nextTenExpected = all.slice(10, 20)

      val firstTenResult =
        messagesRepository.getMessagesPaginated(holder, 10, Option.empty).run(TraceId.generateYOLO).unsafeRunSync()
      firstTenResult.toOption.value.map(_.id) must matchTo(firstTenExpected)

      val nextTenResult =
        messagesRepository
          .getMessagesPaginated(holder, 10, Some(firstTenExpected.last))
          .run(TraceId.generateYOLO)
          .unsafeRunSync()
      nextTenResult.toOption.value.map(_.id) must matchTo(nextTenExpected)
    }
  }
}
