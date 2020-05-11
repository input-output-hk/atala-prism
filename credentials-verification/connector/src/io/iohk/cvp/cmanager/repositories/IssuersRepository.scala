package io.iohk.cvp.cmanager.repositories

import cats.effect.IO
import doobie.implicits._
import doobie.util.transactor.Transactor
import io.iohk.cvp.cmanager.models.Issuer
import io.iohk.cvp.cmanager.repositories.IssuersRepository.IssuerCreationData
import io.iohk.cvp.cmanager.repositories.daos.IssuersDAO
import io.iohk.cvp.utils.FutureEither
import io.iohk.cvp.utils.FutureEither.FutureEitherOps

import scala.concurrent.{ExecutionContext, Future}

object IssuersRepository {
  case class IssuerCreationData(id: Issuer.Id)
}

class IssuersRepository(xa: Transactor[IO])(implicit ec: ExecutionContext) {
  def insert(data: IssuerCreationData): FutureEither[Nothing, Unit] = {
    IssuersDAO
      .insert(Issuer(data.id))
      .transact(xa)
      .unsafeToFuture()
      .map(Right(_))
      .toFutureEither
  }

  def findBy(id: Issuer.Id): FutureEither[Nothing, Option[Issuer]] = {
    IssuersDAO
      .findBy(id)
      .transact(xa)
      .unsafeToFuture()
      .map(Right(_))
      .toFutureEither
  }
}
