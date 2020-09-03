package io.iohk.atala.prism.cmanager.models

import java.time.Instant
import java.util.UUID

import io.circe.Json
import io.iohk.atala.prism.connector.model.TokenString
import io.iohk.atala.prism.cmanager.models.requests.CreateSubject
import io.scalaland.chimney.dsl._

case class Subject(
    id: Subject.Id,
    externalId: Subject.ExternalId,
    data: Json,
    createdOn: Instant,
    connectionStatus: Student.ConnectionStatus,
    connectionToken: Option[TokenString],
    connectionId: Option[UUID]
)

object Subject {
  case class Id(value: UUID) extends AnyVal
  case class ExternalId(value: String) extends AnyVal

  object ExternalId {
    def random(): ExternalId = ExternalId(UUID.randomUUID().toString)
  }
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
