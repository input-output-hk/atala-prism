package io.iohk.connector.model

import java.time.Instant
import java.util.{Base64, UUID}

import enumeratum.EnumEntry.Lowercase
import enumeratum._

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

case class ParticipantInfo(id: ParticipantId, tpe: ParticipantType, name: String, did: Option[String])

case class ConnectionInfo(id: ConnectionId, instantiatedAt: Instant, participantInfo: ParticipantInfo)

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
