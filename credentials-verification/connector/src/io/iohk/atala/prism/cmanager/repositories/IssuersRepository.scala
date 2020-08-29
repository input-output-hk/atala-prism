package io.iohk.atala.prism.cmanager.repositories

import cats.effect.IO
import doobie.implicits._
import doobie.util.transactor.Transactor
import io.iohk.atala.prism.cmanager.models.Issuer
import io.iohk.atala.prism.cmanager.repositories.IssuersRepository.IssuerCreationData
import io.iohk.atala.prism.cmanager.repositories.daos.IssuersDAO
import io.iohk.atala.prism.utils.FutureEither
import io.iohk.atala.prism.utils.FutureEither.FutureEitherOps

import scala.concurrent.ExecutionContext

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
