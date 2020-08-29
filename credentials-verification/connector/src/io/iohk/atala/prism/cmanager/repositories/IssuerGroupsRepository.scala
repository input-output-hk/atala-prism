package io.iohk.atala.prism.cmanager.repositories

import cats.effect.IO
import doobie.implicits._
import doobie.util.transactor.Transactor
import io.iohk.atala.prism.cmanager.models.{Issuer, IssuerGroup}
import io.iohk.atala.prism.cmanager.repositories.daos.IssuerGroupsDAO
import io.iohk.atala.prism.utils.FutureEither
import io.iohk.atala.prism.utils.FutureEither.FutureEitherOps

import scala.concurrent.ExecutionContext

class IssuerGroupsRepository(xa: Transactor[IO])(implicit ec: ExecutionContext) {
  def create(issuer: Issuer.Id, name: IssuerGroup.Name): FutureEither[Nothing, IssuerGroup] = {
    IssuerGroupsDAO
      .create(issuer, name)
      .transact(xa)
      .unsafeToFuture()
      .map(Right(_))
      .toFutureEither
  }

  def getBy(issuer: Issuer.Id): FutureEither[Nothing, List[IssuerGroup.Name]] = {
    IssuerGroupsDAO
      .getBy(issuer)
      .transact(xa)
      .unsafeToFuture()
      .map(Right(_))
      .toFutureEither
  }
}
