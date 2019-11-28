package io.iohk.connector.repositories

import java.time.Instant

import doobie.implicits._
import doobie.util.{Read, fragment}
import io.iohk.connector.model._
import io.iohk.connector.repositories.daos._
import io.iohk.cvp.repositories.PostgresRepositorySpec

abstract class ConnectorRepositorySpecBase extends PostgresRepositorySpec {
  override val tables = List("messages", "connections", "connection_tokens", "holder_public_keys", "participants")
  implicit class SqlTestOps(val sql: fragment.Fragment) {
    def runUpdate(): Unit = {
      sql.update.run.transact(database).unsafeToFuture().futureValue
    }
    def runUnique[T: Read](): T = {
      sql.query[T].unique.transact(database).unsafeToFuture().futureValue
    }
  }

  protected def createParticipant(tpe: ParticipantType, name: String, did: String): ParticipantId = {
    sql"""INSERT INTO participants(id, tpe, did, name) VALUES (${ParticipantId
      .random()}, $tpe, $did, $name) RETURNING id"""
      .runUnique[ParticipantId]
  }

  protected def createIssuer(name: String = "Issuer"): ParticipantId = {
    createParticipant(ParticipantType.Issuer, name, s"did:test:${name.toLowerCase}")
  }

  protected def createHolder(name: String = "Holder"): ParticipantId = {
    createParticipant(ParticipantType.Holder, name, s"did:test:${name.toLowerCase}")
  }

  protected def createVerifier(name: String = "Verifier"): ParticipantId = {
    createParticipant(ParticipantType.Verifier, name, s"did:test:${name.toLowerCase}")
  }

  protected def createConnection(initiatorId: ParticipantId, acceptorId: ParticipantId): ConnectionId = {
    val token = createToken(initiatorId)
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
