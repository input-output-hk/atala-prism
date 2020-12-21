package io.iohk.atala.prism.connector.repositories

import java.time.Instant

import doobie.implicits._
import doobie.implicits.legacy.instant._
import io.iohk.atala.prism.crypto.ECPublicKey
import io.iohk.atala.prism.connector.model._
import io.iohk.atala.prism.connector.repositories.daos._
import io.iohk.atala.prism.identity.DID
import io.iohk.atala.prism.models.ParticipantId
import io.iohk.atala.prism.repositories.PostgresRepositorySpec
import io.iohk.atala.prism.repositories.ops.SqlTestOps.Implicits

trait ConnectorRepositorySpecBase extends PostgresRepositorySpec {
  protected def createParticipant(
      tpe: ParticipantType,
      name: String,
      did: DID,
      publicKey: Option[ECPublicKey],
      logo: Option[ParticipantLogo]
  ): ParticipantId = {
    sql"""INSERT INTO participants(id, tpe, did, public_key, name, logo) VALUES
          (${ParticipantId.random()}, $tpe, $did, $publicKey, $name, $logo)
          RETURNING id"""
      .runUnique[ParticipantId]()
  }

  protected def createIssuer(name: String = "Issuer", logo: Option[ParticipantLogo] = None): ParticipantId = {
    createParticipant(ParticipantType.Issuer, name, DID.buildPrismDID(name.toLowerCase), None, logo)
  }

  protected def createHolder(name: String = "Holder", publicKey: Option[ECPublicKey] = None): ParticipantId = {
    createParticipant(ParticipantType.Holder, name, DID.buildPrismDID(name.toLowerCase), publicKey, None)
  }

  protected def createVerifier(name: String = "Verifier", logo: Option[ParticipantLogo] = None): ParticipantId = {
    createParticipant(ParticipantType.Verifier, name, DID.buildPrismDID(name.toLowerCase), None, logo)
  }

  protected def createConnection(initiatorId: ParticipantId, acceptorId: ParticipantId): ConnectionId = {
    createConnection(initiatorId, acceptorId, createToken(initiatorId), ConnectionStatus.InvitationMissing)
  }

  protected def createConnection(
      initiatorId: ParticipantId,
      acceptorId: ParticipantId,
      token: TokenString,
      status: ConnectionStatus
  ): ConnectionId = {
    sql"""
         |INSERT INTO connections (id, initiator, acceptor, instantiated_at, token, status)
         |VALUES(${ConnectionId.random()}, $initiatorId, $acceptorId,
         |       now(), $token, $status::CONTACT_CONNECTION_STATUS_TYPE)
         |RETURNING id""".stripMargin.runUnique[ConnectionId]()
  }

  protected def createConnection(
      initiatorId: ParticipantId,
      acceptorId: ParticipantId,
      instantiatedAt: Instant
  ): ConnectionId = {
    val token = createToken(initiatorId)
    val status: ConnectionStatus = ConnectionStatus.InvitationMissing
    sql"""
         |INSERT INTO connections (id, initiator, acceptor, instantiated_at, token, status)
         |VALUES(${ConnectionId.random()}, $initiatorId, $acceptorId,
         |       $instantiatedAt, $token, $status::CONTACT_CONNECTION_STATUS_TYPE)
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
