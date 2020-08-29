package io.iohk.connector.repositories

import java.time.Instant

import doobie.implicits._
import doobie.util.{Read, fragment}
import io.iohk.atala.crypto.ECPublicKey
import io.iohk.connector.model._
import io.iohk.connector.repositories.daos._
import io.iohk.atala.prism.models.ParticipantId
import io.iohk.atala.prism.repositories.PostgresRepositorySpec

abstract class ConnectorRepositorySpecBase extends PostgresRepositorySpec {

  implicit class SqlTestOps(val sql: fragment.Fragment) {
    def runUpdate(): Unit = {
      sql.update.run.transact(database).unsafeRunSync()
      ()
    }
    def runUnique[T: Read](): T = {
      sql.query[T].unique.transact(database).unsafeRunSync()
    }
  }

  protected def createParticipant(
      tpe: ParticipantType,
      name: String,
      did: String,
      publicKey: Option[ECPublicKey],
      logo: Option[ParticipantLogo]
  ): ParticipantId = {
    sql"""INSERT INTO participants(id, tpe, did, public_key, name, logo) VALUES
          (${ParticipantId.random()}, $tpe, $did, $publicKey, $name, $logo)
          RETURNING id"""
      .runUnique[ParticipantId]
  }

  protected def createIssuer(name: String = "Issuer", logo: Option[ParticipantLogo] = None): ParticipantId = {
    createParticipant(ParticipantType.Issuer, name, s"did:test:${name.toLowerCase}", None, logo)
  }

  protected def createHolder(name: String = "Holder", publicKey: Option[ECPublicKey] = None): ParticipantId = {
    createParticipant(ParticipantType.Holder, name, s"did:test:${name.toLowerCase}", publicKey, None)
  }

  protected def createVerifier(name: String = "Verifier", logo: Option[ParticipantLogo] = None): ParticipantId = {
    createParticipant(ParticipantType.Verifier, name, s"did:test:${name.toLowerCase}", None, logo)
  }

  protected def createConnection(initiatorId: ParticipantId, acceptorId: ParticipantId): ConnectionId = {
    createConnection(initiatorId, acceptorId, createToken(initiatorId))
  }

  protected def createConnection(
      initiatorId: ParticipantId,
      acceptorId: ParticipantId,
      token: TokenString
  ): ConnectionId = {
    sql"""
         |INSERT INTO connections (id, initiator, acceptor, instantiated_at, token)
         |VALUES(${ConnectionId.random()}, $initiatorId, $acceptorId, now(), $token)
         |RETURNING id""".stripMargin.runUnique[ConnectionId]()
  }

  protected def createConnection(
      initiatorId: ParticipantId,
      acceptorId: ParticipantId,
      instantiatedAt: Instant
  ): ConnectionId = {
    val token = createToken(initiatorId)
    sql"""
         |INSERT INTO connections (id, initiator, acceptor, instantiated_at, token)
         |VALUES(${ConnectionId.random()}, $initiatorId, $acceptorId, $instantiatedAt, $token)
         |RETURNING id""".stripMargin.runUnique[ConnectionId]()
  }

  protected def createMessage(
      connection: ConnectionId,
      sender: ParticipantId,
      recipient: ParticipantId,
      receivedAt: Instant,
      content: Array[Byte]
  ): MessageId = {
    sql"""
         |INSERT INTO messages (id, connection, sender, recipient, received_at, content)
         |VALUES (${MessageId.random()}, $connection, $sender, $recipient, $receivedAt, $content)
         |RETURNING id""".stripMargin.runUnique[MessageId]()
  }

  protected def createToken(initiator: ParticipantId): TokenString = {
    val tokenString = TokenString.random()
    ConnectionTokensDAO
      .insert(initiator, tokenString)
      .transact(database)
      .unsafeToFuture()
      .futureValue

    tokenString
  }
}
