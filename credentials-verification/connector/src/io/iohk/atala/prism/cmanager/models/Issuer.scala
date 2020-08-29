package io.iohk.atala.prism.cmanager.models

import java.util.UUID

case class Issuer(id: Issuer.Id)

object Issuer {
  case class Id(value: UUID) extends AnyVal
}
