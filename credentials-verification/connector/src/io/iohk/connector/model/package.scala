package io.iohk.connector.model

import java.time.Instant
import java.util.{Base64, UUID}

import enumeratum.EnumEntry.Lowercase
import enumeratum._

import io.iohk.connector.protos

import scala.util.Random

sealed trait ParticipantType extends EnumEntry with Lowercase
object ParticipantType extends Enum[ParticipantType] {
  val values = findValues

  case object Issuer extends ParticipantType
  case object Holder extends ParticipantType
  case object Verifier extends ParticipantType
}

case class ParticipantId(id: UUID) extends AnyVal

object ParticipantId {
  def random(): ParticipantId = {
    new ParticipantId(UUID.randomUUID())
  }
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

case class ParticipantInfo(id: ParticipantId, tpe: ParticipantType, name: String, did: Option[String]) {
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
            protos.IssuerInfo(did.getOrElse(""), name)
          )
        )
    }
  }
}

case class ConnectionInfo(id: ConnectionId, instantiatedAt: Instant, participantInfo: ParticipantInfo) {
  def toProto: protos.ConnectionInfo = {
    protos.ConnectionInfo(id.id.toString, instantiatedAt.toEpochMilli, participantInfo.toProto)
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

case class Message(id: MessageId, connection: ConnectionId, receivedAt: Instant, content: Array[Byte])
