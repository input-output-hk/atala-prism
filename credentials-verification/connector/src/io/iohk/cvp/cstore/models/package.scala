package io.iohk.cvp.cstore

import java.time.Instant
import java.util.UUID

import enumeratum.EnumEntry.UpperSnakecase
import enumeratum.{EnumEntry, _}
import io.circe.Json
import io.iohk.connector.model.ConnectionId
import io.iohk.cvp.models.ParticipantId

package object models {
  sealed trait IndividualConnectionStatus extends EnumEntry with UpperSnakecase

  object IndividualConnectionStatus extends Enum[IndividualConnectionStatus] {
    val values = findValues

    case object Created extends IndividualConnectionStatus
    case object Invited extends IndividualConnectionStatus
    case object Connected extends IndividualConnectionStatus
    case object Revoked extends IndividualConnectionStatus
  }

  case class Verifier(id: Verifier.Id)

  object Verifier {
    case class Id(uuid: UUID) extends AnyVal
  }

  case class StoreIndividual(
      id: ParticipantId,
      status: IndividualConnectionStatus,
      connectionToken: Option[String],
      connectionId: Option[ConnectionId],
      fullName: String,
      email: Option[String],
      createdAt: Instant
  )

  case class VerifierHolder(
      id: VerifierHolder.Id,
      data: Json,
      status: IndividualConnectionStatus,
      connectionToken: Option[String],
      connectionId: Option[ConnectionId],
      createdAt: Instant
  )

  object VerifierHolder {
    case class Id(uuid: UUID) extends AnyVal
  }

  case class SHA256Digest(value: Array[Byte]) {
    require(value.length == 32)

    def hexValue: String = value.map("%02x".format(_)).mkString("")

    override def canEqual(that: Any): Boolean = that.isInstanceOf[SHA256Digest]

    override def equals(obj: Any): Boolean = {
      canEqual(obj) && (obj match {
        case SHA256Digest(otherValue) => value.sameElements(otherValue)
        case _ => false
      })
    }
  }

  case class StoredSignedCredential(
      individualId: ParticipantId,
      encodedSignedCredential: String,
      storedAt: Instant
  )
}
