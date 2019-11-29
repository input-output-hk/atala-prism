package io.iohk.connector.repositories

import java.time.{Instant, LocalDateTime, ZoneOffset}

import com.softwaremill.diffx.scalatest.DiffMatcher._
import doobie.implicits._
import org.scalatest.EitherValues._
import io.iohk.connector.repositories.daos._
import io.iohk.cvp.models.ParticipantId

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

  "getMessagesPaginated" should {
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

      val all = messagesRepository
        .getMessagesPaginated(holder, 20, Option.empty)
        .value
        .futureValue
        .right
        .value
        .map(_.id)

      val firstTenExpected = all.take(10)
      val nextTenExpected = all.drop(10).take(10)

      val firstTenResult = messagesRepository.getMessagesPaginated(holder, 10, Option.empty).value.futureValue
      firstTenResult.right.value.map(_.id) must matchTo(firstTenExpected)

      val nextTenResult =
        messagesRepository.getMessagesPaginated(holder, 10, Some(firstTenExpected.last)).value.futureValue
      nextTenResult.right.value.map(_.id) must matchTo(nextTenExpected)
    }
  }
}
