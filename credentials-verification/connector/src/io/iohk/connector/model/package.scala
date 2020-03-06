package io.iohk.connector.model

import java.time.Instant
import java.util.{Base64, UUID}

import com.google.protobuf.ByteString
import enumeratum.EnumEntry.Lowercase
import enumeratum._
import io.iohk.cvp.crypto.ECKeys.EncodedPublicKey
import io.iohk.cvp.models.ParticipantId
import io.iohk.prism.protos.connector_models

import scala.util.Random

sealed trait ParticipantType extends EnumEntry with Lowercase
object ParticipantType extends Enum[ParticipantType] {
  val values = findValues

  case object Issuer extends ParticipantType
  case object Holder extends ParticipantType
  case object Verifier extends ParticipantType
}

case class ConnectionId(id: UUID) extends AnyVal

object ConnectionId {
  def random(): ConnectionId = {
    new ConnectionId(UUID.randomUUID())
  }
}

case class MessageId(id: UUID) extends AnyVal

object MessageId {
  def random(): MessageId = {
    new MessageId(UUID.randomUUID())
  }
}

case class ParticipantLogo(bytes: Vector[Byte]) extends AnyVal
case class ParticipantInfo(
    id: ParticipantId,
    tpe: ParticipantType,
    publicKey: Option[EncodedPublicKey],
    name: String,
    did: Option[String],
    logo: Option[ParticipantLogo]
) {

  def toProto: connector_models.ParticipantInfo = {
    tpe match {
      case ParticipantType.Holder =>
        connector_models.ParticipantInfo(
          connector_models.ParticipantInfo.Participant.Holder(
            connector_models.HolderInfo(did.getOrElse(""), name)
          )
        )
      case ParticipantType.Issuer =>
        connector_models.ParticipantInfo(
          connector_models.ParticipantInfo.Participant.Issuer(
            connector_models.IssuerInfo(
              dID = did.getOrElse(""),
              name = name,
              logo = ByteString.copyFrom(logo.map(_.bytes).getOrElse(Vector.empty).toArray)
            )
          )
        )
      case ParticipantType.Verifier =>
        connector_models.ParticipantInfo(
          connector_models.ParticipantInfo.Participant.Verifier(
            connector_models.VerifierInfo(
              dID = did.getOrElse(""),
              name = name,
              logo = ByteString.copyFrom(logo.map(_.bytes).getOrElse(Vector.empty).toArray)
            )
          )
        )
    }
  }
}

case class ConnectionInfo(
    id: ConnectionId,
    instantiatedAt: Instant,
    participantInfo: ParticipantInfo,
    token: TokenString
) {
  def toProto: connector_models.ConnectionInfo = {
    connector_models.ConnectionInfo(
      id.id.toString,
      created = instantiatedAt.toEpochMilli,
      participantInfo = Some(participantInfo.toProto),
      token = token.token
    )
  }
}

case class Connection(connectionToken: String) {
  def toProto: connector_models.Connection = {
    connector_models.Connection(connectionToken)
  }
}

class TokenString(val token: String) extends AnyVal

object TokenString {
  def random(randomness: Random): TokenString = {
    val bytes = Array.ofDim[Byte](16)
    randomness.nextBytes(bytes)
    new TokenString(Base64.getUrlEncoder.encodeToString(bytes))
  }

  def random(): TokenString = random(Random)
}

case class Message(id: MessageId, connection: ConnectionId, receivedAt: Instant, content: Array[Byte]) {
  def toProto: connector_models.ReceivedMessage = {
    connector_models.ReceivedMessage(
      id.id.toString,
      receivedAt.toEpochMilli,
      connection.id.toString,
      ByteString.copyFrom(content)
    )
  }
}

case class RequestNonce(bytes: Vector[Byte]) extends AnyVal
