package io.iohk.cvp.cmanager.models

import java.time.Instant
import java.util.UUID

import io.circe.Json
import io.iohk.connector.model.TokenString
import io.iohk.cvp.cmanager.models.requests.CreateSubject
import io.scalaland.chimney.dsl._

case class Subject(
    id: Subject.Id,
    data: Json,
    createdOn: Instant,
    connectionStatus: Student.ConnectionStatus,
    connectionToken: Option[TokenString],
    connectionId: Option[UUID],
    groupName: IssuerGroup.Name
)

object Subject {
  case class Id(value: UUID) extends AnyVal

  def create(data: CreateSubject, id: Id, createdOn: Instant, connectionStatus: Student.ConnectionStatus): Subject = {
    data
      .into[Subject]
      .withFieldConst(_.id, id)
      .withFieldConst(_.createdOn, createdOn)
      .withFieldConst(_.connectionStatus, connectionStatus)
      .withFieldConst(_.connectionId, None)
      .withFieldConst(_.connectionToken, None)
      .transform
  }
}
