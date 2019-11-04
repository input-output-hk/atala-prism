package io.iohk.cvp.cmanager.models

import java.util.UUID

case class Issuer(id: Issuer.Id, name: Issuer.Name, did: String)

object Issuer {
  case class Id(value: UUID) extends AnyVal
  case class Name(value: String) extends AnyVal
}
