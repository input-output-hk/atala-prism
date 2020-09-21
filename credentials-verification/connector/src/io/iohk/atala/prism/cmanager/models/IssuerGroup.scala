package io.iohk.atala.prism.cmanager.models

import java.util.UUID

import io.iohk.atala.prism.cmanager.models.IssuerGroup._
import io.iohk.atala.prism.console.models.Institution

case class IssuerGroup(id: Id, name: Name, issuerId: Institution.Id)

object IssuerGroup {

  case class Id(value: UUID) extends AnyVal
  case class Name(value: String) extends AnyVal

}
