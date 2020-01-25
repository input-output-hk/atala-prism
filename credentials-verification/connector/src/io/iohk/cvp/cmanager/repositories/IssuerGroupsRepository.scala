package io.iohk.cvp.cmanager.repositories

import cats.effect.IO
import doobie.implicits._
import doobie.util.transactor.Transactor
import io.iohk.cvp.cmanager.models.{Issuer, IssuerGroup}
import io.iohk.cvp.cmanager.repositories.daos.IssuerGroupsDAO
import io.iohk.cvp.utils.FutureEither
import io.iohk.cvp.utils.FutureEither.FutureEitherOps

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
