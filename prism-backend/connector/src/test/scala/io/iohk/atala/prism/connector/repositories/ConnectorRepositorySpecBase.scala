package io.iohk.atala.prism.connector.repositories

import java.time.Instant
import doobie.implicits._
import doobie.implicits.legacy.instant._
import io.iohk.atala.prism.kotlin.crypto.EC
import io.iohk.atala.prism.kotlin.crypto.keys.ECPublicKey
import io.iohk.atala.prism.connector.model._
import io.iohk.atala.prism.connector.repositories.daos._
import io.iohk.atala.prism.kotlin.identity.PrismDid
import io.iohk.atala.prism.models.ParticipantId
import io.iohk.atala.prism.AtalaWithPostgresSpec
import io.iohk.atala.prism.connector.DataPreparation
import io.iohk.atala.prism.repositories.ops.SqlTestOps.Implicits

trait ConnectorRepositorySpecBase extends AtalaWithPostgresSpec {
  protected def createParticipant(
      tpe: ParticipantType,
      name: String,
      did: PrismDid,
      publicKey: Option[ECPublicKey],
      logo: Option[ParticipantLogo]
  ): ParticipantId = {
    sql"""INSERT INTO participants(id, tpe, did, public_key, name, logo) VALUES
          (${ParticipantId.random()}, $tpe, $did, $publicKey, $name, $logo)
          RETURNING id"""
      .runUnique[ParticipantId]()
  }

  protected def createIssuer(
      name: String = "Issuer",
      logo: Option[ParticipantLogo] = None,
      did: PrismDid = DataPreparation.newDID()
  ): ParticipantId = {
    createParticipant(
      ParticipantType.Issuer,
      name,
      did,
      None,
      logo
    )
  }

  protected def createHolder(
      name: String = "Holder",
      publicKey: Option[ECPublicKey] = None,
      did: PrismDid = DataPreparation.newDID()
  ): ParticipantId = {
    createParticipant(
      ParticipantType.Holder,
      name,
      did,
      publicKey,
      None
    )
  }

  protected def createVerifier(name: String = "Verifier", logo: Option[ParticipantLogo] = None): ParticipantId = {
    createParticipant(
      ParticipantType.Verifier,
      name,
      PrismDid.buildLongFormFromMasterKey(EC.generateKeyPair().getPublicKey),
      None,
      logo
    )
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
      .insert(initiator, List(tokenString))
      .transact(database)
      .unsafeToFuture()
      .futureValue

    tokenString
  }
}
