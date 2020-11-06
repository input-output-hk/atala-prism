package io.iohk.atala.prism.management.console.repositories.daos

import doobie.ConnectionIO
import doobie.implicits.toSqlInterpolator
import io.iohk.atala.prism.management.console.models.Contact

object ContactsDAO {
  def createContact(id: Contact.Id): ConnectionIO[Contact] = {
    sql"""
         |INSERT INTO contacts (contact_id)
         |VALUES ($id)
         |RETURNING contact_id
         |""".stripMargin.query[Contact].unique
  }
}
