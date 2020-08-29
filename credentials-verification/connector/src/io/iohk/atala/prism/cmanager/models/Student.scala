package io.iohk.atala.prism.cmanager.models

import java.time.{Instant, LocalDate}
import java.util.UUID

import enumeratum.{Enum, EnumEntry}
import io.iohk.atala.prism.cmanager.models.requests.CreateStudent
import io.iohk.connector.model.TokenString
import io.iohk.atala.prism.cmanager.models.requests.CreateStudent
import io.scalaland.chimney.dsl._

case class Student(
    id: Student.Id,
    universityAssignedId: String,
    fullName: String,
    email: String,
    admissionDate: LocalDate,
    createdOn: Instant,
    connectionStatus: Student.ConnectionStatus,
    connectionToken: Option[TokenString],
    connectionId: Option[UUID],
    groupName: IssuerGroup.Name
)

object Student {
  case class Id(value: UUID) extends AnyVal

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

  def create(data: CreateStudent, id: Id, createdOn: Instant, connectionStatus: ConnectionStatus): Student = {
    data
      .into[Student]
      .withFieldConst(_.id, id)
      .withFieldConst(_.createdOn, createdOn)
      .withFieldConst(_.connectionStatus, connectionStatus)
      .withFieldConst(_.connectionId, None)
      .withFieldConst(_.connectionToken, None)
      .transform
  }
}
