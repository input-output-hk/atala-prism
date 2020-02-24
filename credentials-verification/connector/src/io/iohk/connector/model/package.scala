package io.iohk.connector.model

import java.time.Instant
import java.util.{Base64, UUID}

import com.google.protobuf.ByteString
import enumeratum.EnumEntry.Lowercase
import enumeratum._
import io.iohk.cvp.connector.protos
import io.iohk.cvp.crypto.ECKeys.EncodedPublicKey
import io.iohk.cvp.models.ParticipantId

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

  def toProto: protos.ParticipantInfo = {
    tpe match {
      case ParticipantType.Holder =>
        protos.ParticipantInfo(
          protos.ParticipantInfo.Participant.Holder(
            protos.HolderInfo(did.getOrElse(""), name)
          )
        )
      case ParticipantType.Issuer =>
        protos.ParticipantInfo(
          protos.ParticipantInfo.Participant.Issuer(
            protos.IssuerInfo(
              dID = did.getOrElse(""),
              name = name,
              logo = ByteString.copyFrom(logo.map(_.bytes).getOrElse(Vector.empty).toArray)
            )
          )
        )
      case ParticipantType.Verifier =>
        protos.ParticipantInfo(
          protos.ParticipantInfo.Participant.Verifier(
            protos.VerifierInfo(
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
  def toProto: protos.ConnectionInfo = {
    protos.ConnectionInfo(
      id.id.toString,
      created = instantiatedAt.toEpochMilli,
      participantInfo = Some(participantInfo.toProto),
      token = token.token
    )
  }
}

case class Connection(connectionToken: String) {
  def toProto: protos.Connection = {
    protos.Connection(connectionToken)
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
  def toProto: protos.ReceivedMessage = {
    protos.ReceivedMessage(
      id.id.toString,
      receivedAt.toEpochMilli,
      connection.id.toString,
      ByteString.copyFrom(content)
    )
  }
}

case class RequestNonce(bytes: Vector[Byte]) extends AnyVal
