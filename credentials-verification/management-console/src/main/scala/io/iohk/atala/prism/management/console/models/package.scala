package io.iohk.atala.prism.management.console

import java.util.UUID

package object models {
  final case class Contact(
      contactId: Contact.Id
  )

  object Contact {
    final case class Id(value: UUID) extends AnyVal
  }
}
