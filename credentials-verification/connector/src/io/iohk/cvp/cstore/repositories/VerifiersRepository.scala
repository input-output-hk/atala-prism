package io.iohk.cvp.cstore.repositories

import java.util.UUID

import cats.effect.IO
import doobie.implicits._
import doobie.util.transactor.Transactor
import io.iohk.cvp.cstore.models.Verifier
import io.iohk.cvp.cstore.repositories.VerifiersRepository.VerifierCreationData
import io.iohk.cvp.cstore.repositories.daos.VerifiersDAO
import io.iohk.cvp.utils.FutureEither
import io.iohk.cvp.utils.FutureEither.FutureEitherOps

import scala.concurrent.{ExecutionContext, Future}

object VerifiersRepository {
  case class VerifierCreationData(id: Verifier.Id)
}

class VerifiersRepository(xa: Transactor[IO])(implicit ec: ExecutionContext) {
  def insert(data: VerifierCreationData): FutureEither[Nothing, Unit] = {
    VerifiersDAO
      .insert(Verifier(data.id))
      .transact(xa)
      .unsafeToFuture()
      .map(Right(_))
      .toFutureEither
  }

  def findBy(id: Verifier.Id): FutureEither[Nothing, Option[Verifier]] = {
    VerifiersDAO
      .findBy(id)
      .transact(xa)
      .unsafeToFuture()
      .map(Right(_))
      .toFutureEither
  }
}
