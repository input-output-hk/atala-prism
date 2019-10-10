package io.iohk.connector.repositories
import java.time.{Instant, LocalDateTime, ZoneOffset}

import doobie.implicits._
import io.iohk.connector.model.ParticipantId
import io.iohk.connector.repositories.daos.{ConnectionsDAO, MessagesDAO}
import org.scalatest.EitherValues._
import io.iohk.connector.repositories.daos._

import scala.concurrent.duration.DurationLong

class MessagesRepositorySpec extends ConnectorRepositorySpecBase {

  implicit val pc: PatienceConfig = PatienceConfig(20.seconds, 5.millis)
  lazy val messagesRepository = new MessagesRepository(database)

  "insertMessage" should {
    "insert message from the initiator to the acceptor" in {
      val issuer = createIssuer()
      val holder = createHolder()
      val connection = createConnection(issuer, holder)
      val message = "hello".getBytes

      val result = messagesRepository.insertMessage(issuer, connection, message).value.futureValue
      val messageId = result.right.value

      val (sender, recipient, content) =
        sql"""
             |SELECT sender, recipient, content
             |FROM messages
             |WHERE id = $messageId
           """.stripMargin.runUnique[(ParticipantId, ParticipantId, Array[Byte])]

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
      val messageId = result.right.value

      val (sender, recipient, content) =
        sql"""
             |SELECT sender, recipient, content
             |FROM messages
             |WHERE id = $messageId
           """.stripMargin.runUnique[(ParticipantId, ParticipantId, Array[Byte])]

      sender mustBe holder
      recipient mustBe issuer
      content mustBe message
    }
  }

  "getMessagesSince" should {
    "select subset of messages according to since and limit" in {
      val issuer = createIssuer()
      val holder = createHolder()
      val connection = createConnection(issuer, holder)
      val zeroTime = LocalDateTime.of(2019, 10, 10, 12, 14, 17, 5000).toEpochSecond(ZoneOffset.UTC)

      val messageIds = (for (i <- 0 to 20) yield {
        val messageId =
          createMessage(connection, issuer, holder, Instant.ofEpochMilli(zeroTime + i), s"hello$i".getBytes())
        i -> messageId
      }).toMap

      val result = messagesRepository.getMessagesSince(holder, Instant.ofEpochMilli(zeroTime + 5), 10).value.futureValue
      result.right.value.map(_.id).toSet mustBe (5 to 14).map(messageIds).toSet
    }
  }

}
