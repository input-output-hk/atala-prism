package io.iohk.atala.prism.connector.repositories

import java.time.{Instant, LocalDateTime, ZoneOffset}

import com.softwaremill.diffx.scalatest.DiffMatcher._
import doobie.implicits._
import io.iohk.atala.crypto.EC
import io.iohk.atala.prism.connector.model._
import io.iohk.atala.prism.connector.repositories.daos._
import io.iohk.atala.prism.models.ParticipantId
import io.iohk.atala.prism.repositories.ops.SqlTestOps.Implicits
import org.scalatest.EitherValues._

import scala.concurrent.duration.DurationLong
import scala.language.higherKinds

class ConnectionsRepositorySpec extends ConnectorRepositorySpecBase {

  implicit val pc: PatienceConfig = PatienceConfig(20.seconds, 5.millis)
  lazy val connectionsRepository = new ConnectionsRepository.PostgresImpl(database)

  "insertToken" should {
    "correctly generate token" in {
      val issuerId = createIssuer()
      val token = TokenString.random()

      val result = connectionsRepository.insertToken(issuerId, token).value.futureValue
      result mustBe a[Right[_, _]]

      sql"""SELECT COUNT(1) FROM connection_tokens WHERE token=$token""".runUnique[Int]() mustBe 1
    }
  }

  "getTokenInfo" should {
    "return token info" in {
      val issuerId = createIssuer()

      val token = new TokenString("t0k3nc0de")
      sql"""INSERT INTO connection_tokens(token, initiator) VALUES ($token, $issuerId)""".runUpdate()

      val result = connectionsRepository.getTokenInfo(token).value.futureValue
      result.right.value mustBe ParticipantInfo(
        issuerId,
        ParticipantType.Issuer,
        None,
        "Issuer",
        Some("did:test:issuer"),
        None,
        None,
        None
      )
    }
  }

  "addConnectionFromToken" should {
    "add connection from existing token" in {
      val issuerId = createIssuer()
      val publicKey = EC.generateKeyPair().publicKey

      val token = new TokenString("t0k3nc0de")
      sql"""INSERT INTO connection_tokens(token, initiator) VALUES ($token, $issuerId)""".runUpdate()

      val result = connectionsRepository.addConnectionFromToken(token, publicKey).value.futureValue
      val connectionId = result.right.value._2.id

      sql"""SELECT COUNT(1) FROM connections WHERE id=$connectionId""".runUnique[Int]() mustBe 1
      //verify that instantiated_at field is set correctly, to avoid conversion or timezone errors
      sql"""SELECT COUNT(1) FROM connections
           |WHERE id = $connectionId
           |AND instantiated_at > now() - '10 seconds'::interval AND instantiated_at < now()""".stripMargin
        .runUnique[Int]() mustBe 1
      sql"""SELECT COUNT(1) FROM connection_tokens WHERE token=$token AND used_at IS NOT NULL"""
        .runUnique[Int]() mustBe 1
      sql"""SELECT COUNT(1) FROM connection_tokens WHERE token=$token AND used_at IS NULL""".runUnique[Int]() mustBe 0
    }
  }

  "getConnectionsPaginated" should {
    "return both initiated and accepted connections" in {
      val issuerId = createIssuer()
      val holderId = createHolder()
      val verifierId = createVerifier()

      val connections = Seq(
        createConnection(verifierId, holderId),
        createConnection(issuerId, verifierId)
      )

      val result = connectionsRepository.getConnectionsPaginated(verifierId, 10, Option.empty).value.futureValue
      result.right.value.map(_.id).toSet must matchTo(connections.toSet)
    }

    // creates connections (initiator -> acceptor):
    // Verifier -> Holder0 at zeroTime
    // Issuer0 -> Verifier at zeroTime + 1
    // Verifier -> Holder1 at zeroTime + 2
    // ...
    // Issuer12 -> Verifier at zeroTime + 25
    def createExampleConnections(verifierId: ParticipantId, zeroTime: Long): Map[String, ConnectionId] = {
      (for (i <- (0 to 12)) yield {
        val holderName = s"Holder$i"
        val issuerName = s"Issuer$i"
        val holderId = createHolder(holderName)
        val issuerId = createIssuer(issuerName, Some(ParticipantLogo(Vector(10.toByte, 15.toByte))))

        val holderConnectionId = createConnection(verifierId, holderId, Instant.ofEpochMilli(zeroTime + 2 * i))
        val issuerConnectionId = createConnection(issuerId, verifierId, Instant.ofEpochMilli(zeroTime + 2 * i + 1))

        List(holderName -> holderConnectionId, issuerName -> issuerConnectionId)
      }).toList.flatten.toMap
    }

    "select subset of connections according to the last seen connection and limit" in {
      val verifierId = createVerifier()
      val zeroTime = LocalDateTime.of(2019, 10, 8, 20, 12, 17, 5000).toEpochSecond(ZoneOffset.UTC)
      createExampleConnections(verifierId, zeroTime)

      val all = connectionsRepository
        .getConnectionsPaginated(verifierId, 20, Option.empty)
        .value
        .futureValue
        .right
        .value
        .map(_.id)

      val firstTenExpected = all.take(10)
      val nextTenExpected = all.drop(10).take(10)

      val firstTenResult = connectionsRepository.getConnectionsPaginated(verifierId, 10, Option.empty).value.futureValue
      firstTenResult.right.value.map(_.id) must matchTo(firstTenExpected)

      val nextTenResult =
        connectionsRepository.getConnectionsPaginated(verifierId, 10, Some(firstTenExpected.last)).value.futureValue
      nextTenResult.right.value.map(_.id) must matchTo(nextTenExpected)
    }
  }

  "getConnectionByToken" should {
    "return correct token and connectionId" in {
      val h1 = createHolder("h1", None)
      val h2 = createHolder("h2", None)
      val token = createToken(h1)
      val connectionId: ConnectionId = createConnection(h1, h2, token)

      val connection: Connection = connectionsRepository.getConnectionByToken(token).value.futureValue.right.value.get

      connection.connectionId mustBe connectionId
      connection.connectionToken mustBe token
    }
  }
}
