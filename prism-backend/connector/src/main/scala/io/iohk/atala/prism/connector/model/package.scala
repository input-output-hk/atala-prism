package io.iohk.atala.prism.connector.model

import cats.syntax.option._
import com.google.protobuf.ByteString
import derevo.derive
import enumeratum.EnumEntry.Lowercase
import enumeratum._
import io.iohk.atala.prism.crypto.keys.ECPublicKey
import io.iohk.atala.prism.connector.AtalaOperationId
import io.iohk.atala.prism.identity.{PrismDid => DID}
import io.iohk.atala.prism.models.{ParticipantId, UUIDValue}
import io.iohk.atala.prism.protos.connector_models
import io.iohk.atala.prism.utils.syntax._
import tofu.logging.derivation.loggable

import java.time.Instant
import java.util.{Base64, UUID}
import scala.util.Random

sealed trait ParticipantType extends EnumEntry with Lowercase
object ParticipantType extends Enum[ParticipantType] {
  val values = findValues

  case object Issuer extends ParticipantType
  case object Holder extends ParticipantType
  case object Verifier extends ParticipantType
}

@derive(loggable)
case class ConnectionId(uuid: UUID) extends AnyVal with UUIDValue
object ConnectionId extends UUIDValue.Builder[ConnectionId]

@derive(loggable)
case class MessageId(uuid: UUID) extends AnyVal with UUIDValue
object MessageId extends UUIDValue.Builder[MessageId]

case class UpdateParticipantProfile(name: String, logo: Option[ParticipantLogo])

case class ParticipantLogo(bytes: Vector[Byte]) extends AnyVal
case class ParticipantInfo(
    id: ParticipantId,
    tpe: ParticipantType,
    publicKey: Option[ECPublicKey],
    name: String,
    did: Option[DID],
    logo: Option[ParticipantLogo],
    operationId: Option[AtalaOperationId]
)

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
      created = instantiatedAt.toProtoTimestamp.some,
      token = token.token,
      participantName = participantInfo.name,
      participantLogo = ByteString.copyFrom(
        participantInfo.logo.map(_.bytes).getOrElse(Vector.empty).toArray
      ),
      participantDid = participantInfo.did.map(_.getValue).getOrElse("")
    )
  }
}

case class Connection(
    connectionToken: TokenString,
    connectionId: ConnectionId
) {
  def toProto: connector_models.Connection = {
    connector_models.Connection(
      connectionToken = connectionToken.token,
      connectionId = connectionId.toString
    )
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

@derive(loggable)
case class TokenString(token: String) extends AnyVal

object TokenString {
  def random(): TokenString = random(Random)

  def random(randomness: Random): TokenString = {
    val bytes = Array.ofDim[Byte](16)
    randomness.nextBytes(bytes)
    new TokenString(Base64.getUrlEncoder.encodeToString(bytes))
  }
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
      connection.toString,
      ByteString.copyFrom(content),
      receivedAt.toProtoTimestamp.some
    )
  }
}

case class CreateMessage(
    id: MessageId,
    connection: ConnectionId,
    sender: ParticipantId,
    recipient: ParticipantId,
    receivedAt: Instant,
    content: Array[Byte]
)
