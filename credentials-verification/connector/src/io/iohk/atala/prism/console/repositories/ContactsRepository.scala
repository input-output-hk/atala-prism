package io.iohk.atala.prism.console.repositories

import cats.effect.IO
import doobie.implicits._
import doobie.util.transactor.Transactor
import io.iohk.atala.prism.connector.errors.ConnectorError
import io.iohk.atala.prism.console.models.{Contact, CreateContact, Institution}
import io.iohk.atala.prism.console.repositories.daos.ContactsDAO
import io.iohk.atala.prism.utils.FutureEither
import io.iohk.atala.prism.utils.FutureEither.FutureEitherOps

import scala.concurrent.ExecutionContext

class ContactsRepository(xa: Transactor[IO])(implicit ec: ExecutionContext) {
  def create(contactData: CreateContact): FutureEither[ConnectorError, Contact] = {
    ContactsDAO
      .createContact(contactData)
      .transact(xa)
      .unsafeToFuture()
      .map(Right(_))
      .toFutureEither
  }

  def list(
      createdBy: Institution.Id,
      lastSeen: Option[Contact.Id],
      limit: Int
  ): FutureEither[ConnectorError, Seq[Contact]] = {
    ContactsDAO
      .getBy(createdBy, lastSeen, limit, None)
      .transact(xa)
      .unsafeToFuture()
      .map(Right(_))
      .toFutureEither
  }
}
