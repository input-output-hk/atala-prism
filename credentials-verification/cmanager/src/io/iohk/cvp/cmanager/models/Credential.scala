package io.iohk.cvp.cmanager.models

import java.time.{Instant, LocalDate}
import java.util.UUID

import io.iohk.cvp.cmanager.models.requests.CreateCredential
import io.scalaland.chimney.dsl._

case class Credential(
    id: Credential.Id,
    createdOn: Instant,
    issuedBy: Issuer.Id,
    subject: String,
    title: String,
    enrollmentDate: LocalDate,
    graduationDate: LocalDate,
    groupName: String
)

object Credential {

  case class Id(value: UUID) extends AnyVal

  def create(id: Id, createdOn: Instant, data: CreateCredential): Credential = {
    data
      .into[Credential]
      .withFieldConst(_.id, id)
      .withFieldConst(_.createdOn, createdOn)
      .transform
  }
}
