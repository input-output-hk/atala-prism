package io.iohk.atala.prism.vault

import java.util.UUID

package object model {
  final case class Payload(id: Payload.Id)

  object Payload {
    final case class Id(value: UUID) extends AnyVal
  }
}
