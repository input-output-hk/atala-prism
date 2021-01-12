package io.iohk.atala.cvp.webextension.common.models

import enumeratum._

sealed trait Role extends EnumEntry

object Role extends Enum[Role] {
  val values = findValues
  final case object Issuer extends Role
  final case object Verifier extends Role
}
