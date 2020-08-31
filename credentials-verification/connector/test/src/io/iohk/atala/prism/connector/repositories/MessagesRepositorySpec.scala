package io.iohk.atala.prism.connector.repositories

import java.time.{Instant, LocalDateTime, ZoneOffset}

import com.softwaremill.diffx.scalatest.DiffMatcher._
import doobie.implicits._
import io.iohk.atala.prism.connector.model.ConnectionId
import io.iohk.atala.prism.connector.repositories.daos._
import io.iohk.atala.prism.models.ParticipantId
import org.scalatest.EitherValues._

import scala.concurrent.duration.DurationLong
import scala.language.higherKinds

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
        .right
        .value
        .map(_.id)

      val firstTenExpected = all.take(10)
      val nextTenExpected = all.slice(10, 20)

      val firstTenResult = messagesRepository.getMessagesPaginated(holder, 10, Option.empty).value.futureValue
      firstTenResult.right.value.map(_.id) must matchTo(firstTenExpected)

      val nextTenResult =
        messagesRepository.getMessagesPaginated(holder, 10, Some(firstTenExpected.last)).value.futureValue
      nextTenResult.right.value.map(_.id) must matchTo(nextTenExpected)
    }
  }
}
