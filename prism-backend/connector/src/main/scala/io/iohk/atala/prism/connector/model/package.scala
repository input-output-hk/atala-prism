package io.iohk.atala.prism.connector.model

import java.time.Instant
import java.util.{Base64, UUID}
import com.google.protobuf.ByteString
import enumeratum.EnumEntry.Lowercase
import enumeratum._
import io.iohk.atala.prism.crypto.ECPublicKey
import io.iohk.atala.prism.identity.DID
import io.iohk.atala.prism.models.{Ledger, ParticipantId, TransactionId, UUIDValue}
import io.iohk.atala.prism.protos.connector_models

import scala.util.Random

sealed trait ParticipantType extends EnumEntry with Lowercase
object ParticipantType extends Enum[ParticipantType] {
  val values = findValues

  case object Issuer extends ParticipantType
  case object Holder extends ParticipantType
  case object Verifier extends ParticipantType
}

case class ConnectionId(uuid: UUID) extends AnyVal with UUIDValue
object ConnectionId extends UUIDValue.Builder[ConnectionId]

case class MessageId(uuid: UUID) extends AnyVal with UUIDValue
object MessageId extends UUIDValue.Builder[MessageId]

case class ParticipantLogo(bytes: Vector[Byte]) extends AnyVal
case class ParticipantInfo(
    id: ParticipantId,
    tpe: ParticipantType,
    publicKey: Option[ECPublicKey],
    name: String,
    did: Option[DID],
    logo: Option[ParticipantLogo],
    transactionId: Option[TransactionId],
    ledger: Option[Ledger]
) {

  def toProto: connector_models.ParticipantInfo = {
    tpe match {
      case ParticipantType.Holder =>
        connector_models.ParticipantInfo(
          connector_models.ParticipantInfo.Participant.Holder(
            connector_models.HolderInfo(did.map(_.value).getOrElse(""), name)
          )
        )
      case ParticipantType.Issuer =>
        connector_models.ParticipantInfo(
          connector_models.ParticipantInfo.Participant.Issuer(
            connector_models.IssuerInfo(
              dID = did.map(_.value).getOrElse(""),
              name = name,
              logo = ByteString.copyFrom(logo.map(_.bytes).getOrElse(Vector.empty).toArray)
            )
          )
        )
      case ParticipantType.Verifier =>
        connector_models.ParticipantInfo(
          connector_models.ParticipantInfo.Participant.Verifier(
            connector_models.VerifierInfo(
              dID = did.map(_.value).getOrElse(""),
              name = name,
              logo = ByteString.copyFrom(logo.map(_.bytes).getOrElse(Vector.empty).toArray)
            )
          )
        )
    }
  }
}

sealed abstract class ConnectionStatus(value: String) extends EnumEntry {
  override def entryName: String = value
}
object ConnectionStatus extends Enum[ConnectionStatus] {
  lazy val values = findValues

  final case object InvitationMissing extends ConnectionStatus("INVITATION_MISSING")
  final case object ConnectionMissing extends ConnectionStatus("CONNECTION_MISSING")
  final case object ConnectionAccepted extends ConnectionStatus("CONNECTION_ACCEPTED")
  final case object ConnectionRevoked extends ConnectionStatus("CONNECTION_REVOKED")
}

case class ConnectionInfo(
    id: ConnectionId,
    instantiatedAt: Instant,
    participantInfo: ParticipantInfo,
    token: TokenString,
    status: ConnectionStatus
) {
  def toProto: connector_models.ConnectionInfo = {
    connector_models.ConnectionInfo(
      id.toString,
      created = instantiatedAt.toEpochMilli,
      participantInfo = Some(participantInfo.toProto),
      token = token.token,
      participantName = participantInfo.name,
      participantLogo = ByteString.copyFrom(participantInfo.logo.map(_.bytes).getOrElse(Vector.empty).toArray),
      participantDID = participantInfo.did.map(_.value).getOrElse("")
    )
  }
}

case class Connection(connectionToken: TokenString, connectionId: ConnectionId) {
  def toProto: connector_models.Connection = {
    connector_models.Connection(connectionToken = connectionToken.token, connectionId = connectionId.toString)
  }
}

case class RawConnection(
    id: ConnectionId,
    initiator: ParticipantId,
    acceptor: ParticipantId,
    token: TokenString,
    instantiatedAt: Instant,
    status: ConnectionStatus
) {
  def contains(participant: ParticipantId): Boolean = {
    List(acceptor, initiator).contains(participant)
  }
}

case class TokenString(token: String) extends AnyVal

object TokenString {
  def random(randomness: Random): TokenString = {
    val bytes = Array.ofDim[Byte](16)
    randomness.nextBytes(bytes)
    new TokenString(Base64.getUrlEncoder.encodeToString(bytes))
  }

  def random(): TokenString = random(Random)
}

case class Message(
    id: MessageId,
    connection: ConnectionId,
    recipientId: ParticipantId,
    receivedAt: Instant,
    content: Array[Byte]
) {
  def toProto: connector_models.ReceivedMessage = {
    connector_models.ReceivedMessage(
      id.toString,
      receivedAt.toEpochMilli,
      connection.toString,
      ByteString.copyFrom(content)
    )
  }
}
