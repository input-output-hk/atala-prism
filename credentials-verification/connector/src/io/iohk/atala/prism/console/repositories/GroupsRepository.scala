package io.iohk.atala.prism.console.repositories

import cats.effect.IO
import doobie.implicits._
import doobie.util.transactor.Transactor
import io.iohk.atala.prism.console.models.{Institution, IssuerGroup}
import io.iohk.atala.prism.console.repositories.daos.IssuerGroupsDAO
import io.iohk.atala.prism.utils.FutureEither
import io.iohk.atala.prism.utils.FutureEither.FutureEitherOps

import scala.concurrent.ExecutionContext

class GroupsRepository(xa: Transactor[IO])(implicit ec: ExecutionContext) {
  def create(issuer: Institution.Id, name: IssuerGroup.Name): FutureEither[Nothing, IssuerGroup] = {
    IssuerGroupsDAO
      .create(issuer, name)
      .transact(xa)
      .unsafeToFuture()
      .map(Right(_))
      .toFutureEither
  }

  def getBy(issuer: Institution.Id): FutureEither[Nothing, List[IssuerGroup.Name]] = {
    IssuerGroupsDAO
      .getBy(issuer)
      .transact(xa)
      .unsafeToFuture()
      .map(Right(_))
      .toFutureEither
  }
}
