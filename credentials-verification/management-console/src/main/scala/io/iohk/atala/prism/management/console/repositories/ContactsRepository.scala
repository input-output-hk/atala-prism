package io.iohk.atala.prism.management.console.repositories

import cats.effect.IO
import doobie.Transactor
import doobie.implicits._
import io.iohk.atala.prism.management.console.models.Contact
import io.iohk.atala.prism.management.console.repositories.daos.ContactsDAO
import io.iohk.atala.prism.utils.FutureEither
import io.iohk.atala.prism.utils.FutureEither.FutureEitherOps

import scala.concurrent.ExecutionContext

class ContactsRepository(xa: Transactor[IO])(implicit ec: ExecutionContext) {
  def create(id: Contact.Id): FutureEither[Nothing, Contact] = {
    ContactsDAO
      .createContact(id)
      .transact(xa)
      .unsafeToFuture()
      .map(Right(_))
      .toFutureEither
  }
}
