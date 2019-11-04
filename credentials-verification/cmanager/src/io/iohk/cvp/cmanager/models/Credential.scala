package io.iohk.cvp.cmanager.models

import java.time.LocalDate
import java.util.UUID

import io.iohk.cvp.cmanager.models.requests.CreateCredential
import io.scalaland.chimney.dsl._

case class Credential(
    id: Credential.Id,
    issuedBy: Issuer.Id,
    subject: String,
    title: String,
    enrollmentDate: LocalDate,
    graduationDate: LocalDate,
    groupName: String
)

object Credential {

  case class Id(value: UUID) extends AnyVal

  def create(id: Id, data: CreateCredential): Credential = {
    data.into[Credential].withFieldConst(_.id, id).transform
  }
}
