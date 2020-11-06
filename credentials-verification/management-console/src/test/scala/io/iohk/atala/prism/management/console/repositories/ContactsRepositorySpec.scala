package io.iohk.atala.prism.management.console.repositories

import java.util.UUID

import cats.scalatest.EitherMatchers._
import io.iohk.atala.prism.management.console.models.Contact
import io.iohk.atala.prism.repositories.PostgresRepositorySpec

class ContactsRepositorySpec extends PostgresRepositorySpec {
  lazy val repository = new ContactsRepository(database)

  "create" should {
    "create a new payload" in {
      val id = Contact.Id(UUID.randomUUID())
      repository.create(id).value.futureValue must beRight(Contact(id))
    }
  }
}
