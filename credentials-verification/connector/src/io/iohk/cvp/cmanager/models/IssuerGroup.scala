package io.iohk.cvp.cmanager.models

import java.util.UUID

import io.iohk.cvp.cmanager.models.IssuerGroup._

case class IssuerGroup(id: Id, name: Name, issuer: Issuer.Id)

object IssuerGroup {

  case class Id(value: UUID) extends AnyVal
  case class Name(value: String) extends AnyVal

}
