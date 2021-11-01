package io.iohk.atala.prism.connector.repositories

import cats.syntax.either._
import com.softwaremill.diffx.scalatest.DiffMatcher._
import doobie.implicits._
import io.iohk.atala.prism.DIDUtil
import io.iohk.atala.prism.connector.model._
import io.iohk.atala.prism.connector.repositories.daos._
import io.iohk.atala.prism.connector.DataPreparation
import io.iohk.atala.prism.crypto.EC.{INSTANCE => EC}
import io.iohk.atala.prism.logging.TraceId
import io.iohk.atala.prism.models.ParticipantId
import io.iohk.atala.prism.repositories.ops.SqlTestOps._
import io.iohk.atala.prism.utils.IOUtils._
import org.scalatest.Assertion
import org.scalatest.OptionValues._

import java.time.{Instant, LocalDateTime, ZoneOffset}

class ConnectionsRepositorySpec extends ConnectorRepositorySpecBase {
  lazy val connectionsRepository = ConnectionsRepository.unsafe(dbLiftedToTraceIdIO, connectorRepoSpecLogs)
  lazy val contactsRepository = ParticipantsRepository.unsafe(dbLiftedToTraceIdIO, connectorRepoSpecLogs)

  private def checkConnection(connectionId: ConnectionId, token: TokenString): Assertion = {
    sql"""SELECT COUNT(1) FROM connections WHERE id=$connectionId""".runUnique[Int]() mustBe 1
    // verify that instantiated_at field is set correctly, to avoid conversion or timezone errors
    sql"""SELECT COUNT(1) FROM connections
         |WHERE id = $connectionId
         |AND instantiated_at > now() - '10 seconds'::interval AND instantiated_at < now()""".stripMargin
      .runUnique[Int]() mustBe 1
    sql"""SELECT COUNT(1) FROM connection_tokens WHERE token=$token AND used_at IS NOT NULL"""
      .runUnique[Int]() mustBe 1
    sql"""SELECT COUNT(1) FROM connection_tokens WHERE token=$token AND used_at IS NULL""".runUnique[Int]() mustBe 0
  }

  "insertToken" should {
    "correctly generate tokens" in {
      val issuerId = createIssuer()
      val token1 = TokenString.random()
      val token2 = TokenString.random()

      val result =
        connectionsRepository.insertTokens(issuerId, List(token1, token2)).run(TraceId.generateYOLO).unsafeRunSync()
      result.nonEmpty mustBe true

      sql"""SELECT COUNT(1) FROM connection_tokens WHERE token=$token1 OR token=$token2""".runUnique[Int]() mustBe 2
    }
  }

  "getTokenInfo" should {
    "return token info" in {
      val did = DataPreparation.newDID()
      val issuerId = createIssuer(did = did)

      val token = new TokenString("t0k3nc0de")
      sql"""INSERT INTO connection_tokens(token, initiator) VALUES ($token, $issuerId)""".runUpdate()

      val result = connectionsRepository.getTokenInfo(token).run(TraceId.generateYOLO).unsafeRunSync()
      result.toOption.value mustBe ParticipantInfo(
        issuerId,
        ParticipantType.Issuer,
        None,
        "Issuer",
        Some(did),
        None,
        None
      )
    }
  }

  "addConnectionFromToken" should {
    "add connection from existing token using public key auth" in {
      val issuerId = createIssuer()
      val publicKey = EC.generateKeyPair().getPublicKey

      val token = new TokenString("t0k3nc0de")
      sql"""INSERT INTO connection_tokens(token, initiator) VALUES ($token, $issuerId)""".runUpdate()

      val result =
        connectionsRepository.addConnectionFromToken(token, publicKey.asRight).run(TraceId.generateYOLO).unsafeRunSync()
      val connectionId = result.toOption.value.id

      checkConnection(connectionId, token)

      val maybeAcceptorId = ConnectionsDAO.getRawConnection(connectionId).unsafeRun().map(_.acceptor)
      maybeAcceptorId.isDefined mustBe true
      val acceptor = ParticipantsDAO.findByPublicKey(publicKey).value.unsafeRun()
      acceptor.isDefined mustBe true
    }

    "add connection from existing token using unpublished did auth" in {
      val issuerId = createIssuer()
      val (_, did) = DIDUtil.createUnpublishedDid

      val token = new TokenString("t0k3nc0de")
      sql"""INSERT INTO connection_tokens(token, initiator) VALUES ($token, $issuerId)""".runUpdate()

      val result =
        connectionsRepository.addConnectionFromToken(token, did.asLeft).run(TraceId.generateYOLO).unsafeRunSync()
      val connectionId = result.toOption.value.id

      checkConnection(connectionId, token)

      val maybeAcceptorId = ConnectionsDAO.getRawConnection(connectionId).unsafeRun().map(_.acceptor)
      maybeAcceptorId.isDefined mustBe true
      val acceptor = ParticipantsDAO.findByDID(did).value.unsafeRun()
      acceptor.isDefined mustBe true
    }
  }

  "revokeConnection" should {
    def prepare() = {
      val initiator = createIssuer()
      val publicKey = EC.generateKeyPair().getPublicKey

      val token = TokenString.random()

      connectionsRepository
        .insertTokens(initiator, List(token))
        .run(TraceId.generateYOLO)
        .unsafeRunSync()

      val connection = connectionsRepository
        .addConnectionFromToken(token, publicKey.asRight)
        .run(TraceId.generateYOLO)
        .unsafeRunSync()
        .toOption
        .value

      val acceptor = connectionsRepository
        .getOtherSideInfo(connection.id, initiator)
        .run(TraceId.generateYOLO)
        .unsafeRunSync()
        .value
        .id

      (connection.id, initiator, acceptor)
    }

    "work when the initiator revokes the connection" in {
      val (connectionId, initiator, _) = prepare()
      val result =
        connectionsRepository.revokeConnection(initiator, connectionId).run(TraceId.generateYOLO).unsafeRunSync()

      result.isRight must be(true)
    }

    "work when the acceptor revokes the connection" in {
      val (connectionId, _, acceptor) = prepare()
      val result =
        connectionsRepository.revokeConnection(acceptor, connectionId).run(TraceId.generateYOLO).unsafeRunSync()
      result.isRight must be(true)
    }

    "marks the connection as revoked" in {
      val (connectionId, initiator, _) = prepare()
      connectionsRepository.revokeConnection(initiator, connectionId).run(TraceId.generateYOLO).unsafeRunSync()
      val result = ConnectionsDAO
        .getRawConnection(connectionId)
        .unsafeRun()
        .value
        .status
      result must be(ConnectionStatus.ConnectionRevoked)
    }

    "delete the messages related to the connection" in {
      val (connectionId, initiator, acceptor) = prepare()
      MessagesDAO.insert(MessageId.random(), connectionId, initiator, acceptor, "Hello".getBytes).unsafeRun()
      MessagesDAO.insert(MessageId.random(), connectionId, acceptor, initiator, "ACK".getBytes).unsafeRun()
      connectionsRepository.revokeConnection(initiator, connectionId).run(TraceId.generateYOLO).unsafeRunSync()

      MessagesDAO.getConnectionMessages(initiator, connectionId).unsafeRun().isEmpty must be(true)
      MessagesDAO.getConnectionMessages(acceptor, connectionId).unsafeRun().isEmpty must be(true)
    }

    "fail when the participant doesn't belong to the connection" in {
      val (connectionId, _, _) = prepare()

      val initiator = createIssuer()
      val result =
        connectionsRepository.revokeConnection(initiator, connectionId).run(TraceId.generateYOLO).unsafeRunSync()
      result.isLeft must be(true)
    }

    "fail when the connection doesn't exist" in {
      val initiator = createIssuer()
      val result = connectionsRepository
        .revokeConnection(initiator, ConnectionId.random())
        .run(TraceId.generateYOLO)
        .unsafeRunSync()
      result.isLeft must be(true)
    }

    ConnectionStatus.values.filterNot(_ == ConnectionStatus.ConnectionAccepted).foreach { status =>
      s"fail when the connection status is $status" in {
        val (connectionId, initiator, _) = prepare()

        sql"""UPDATE connections SET status = $status::CONTACT_CONNECTION_STATUS_TYPE WHERE id=$connectionId"""
          .runUpdate()
        val result =
          connectionsRepository.revokeConnection(initiator, connectionId).run(TraceId.generateYOLO).unsafeRunSync()
        result.isLeft must be(true)
      }
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

      val result =
        connectionsRepository
          .getConnectionsPaginated(verifierId, 10, Option.empty)
          .run(TraceId.generateYOLO)
          .unsafeRunSync()
      result.toOption.value.map(_.id).toSet must matchTo(connections.toSet)
    }

    // creates connections (initiator -> acceptor):
    // Verifier -> Holder0 at zeroTime
    // Issuer0 -> Verifier at zeroTime + 1
    // Verifier -> Holder1 at zeroTime + 2
    // ...
    // Issuer12 -> Verifier at zeroTime + 25
    def createExampleConnections(verifierId: ParticipantId, zeroTime: Long): Map[String, ConnectionId] = {
      (for (i <- 0 to 12) yield {
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
        .run(TraceId.generateYOLO)
        .unsafeToFuture()
        .futureValue
        .toOption
        .value
        .map(_.id)

      val firstTenExpected = all.take(10)
      val nextTenExpected = all.slice(10, 20)

      val firstTenResult =
        connectionsRepository
          .getConnectionsPaginated(verifierId, 10, Option.empty)
          .run(TraceId.generateYOLO)
          .unsafeRunSync()
      firstTenResult.toOption.value.map(_.id) must matchTo(firstTenExpected)

      val nextTenResult =
        connectionsRepository
          .getConnectionsPaginated(verifierId, 10, Some(firstTenExpected.last))
          .run(TraceId.generateYOLO)
          .unsafeToFuture()
          .futureValue
      nextTenResult.toOption.value.map(_.id) must matchTo(nextTenExpected)
    }
  }

  "getConnectionByToken" should {
    "return correct token and connectionId" in {
      val h1 = createHolder("h1", None)
      val h2 = createHolder("h2", None)
      val token = createToken(h1)
      val connectionId: ConnectionId = createConnection(h1, h2, token, ConnectionStatus.InvitationMissing)

      val connection: Connection =
        connectionsRepository.getConnectionByToken(token).run(TraceId.generateYOLO).unsafeRunSync().value

      connection.connectionId mustBe connectionId
      connection.connectionToken mustBe token
    }
  }

  "getConnectionStatuses" should {
    "return correct list of connection statuses" in {
      val initiator1 = createHolder("initiator1", None)
      val acceptor1 = createHolder("acceptor1", None)
      val token1 = createToken(initiator1)
      val connectionId1 = createConnection(initiator1, acceptor1, token1, ConnectionStatus.InvitationMissing)

      val initiator2 = createHolder("initiator2", None)
      val acceptor2 = createHolder("acceptor2", None)
      val token2 = createToken(initiator2)
      val connectionId2 = createConnection(initiator2, acceptor2, token2, ConnectionStatus.ConnectionAccepted)

      val connectionStatuses =
        connectionsRepository
          .getConnectionsByConnectionTokens(List(token1, token2))
          .run(TraceId.generateYOLO)
          .unsafeRunSync()

      val contactConnection1 = ContactConnection(Some(connectionId1), Some(token1), ConnectionStatus.InvitationMissing)
      val contactConnection2 = ContactConnection(Some(connectionId2), Some(token2), ConnectionStatus.ConnectionAccepted)

      connectionStatuses mustBe List(contactConnection1, contactConnection2)
    }

    "return invitation missing for non-existing connection tokens" in {
      val connectionStatuses =
        connectionsRepository
          .getConnectionsByConnectionTokens(List(TokenString("tokenString1"), TokenString("tokenString2")))
          .run(TraceId.generateYOLO)
          .unsafeToFuture()
          .futureValue

      val contactConnection1 = ContactConnection(None, None, ConnectionStatus.InvitationMissing)
      val contactConnection2 = ContactConnection(None, None, ConnectionStatus.InvitationMissing)

      connectionStatuses mustBe List(contactConnection1, contactConnection2)
    }

    "return connection missing for non-existing connections" in {
      val initiator1 = createHolder("initiator1", None)
      val token1 = createToken(initiator1)

      val initiator2 = createHolder("initiator2", None)
      val token2 = createToken(initiator2)

      val connectionStatuses =
        connectionsRepository
          .getConnectionsByConnectionTokens(List(token1, token2))
          .run(TraceId.generateYOLO)
          .unsafeToFuture()
          .futureValue

      val contactConnection1 = ContactConnection(None, None, ConnectionStatus.ConnectionMissing)
      val contactConnection2 = ContactConnection(None, None, ConnectionStatus.ConnectionMissing)

      connectionStatuses mustBe List(contactConnection1, contactConnection2)
    }
  }
}
