package io.iohk.atala.prism.node.repositories

import cats.effect.{MonadCancelThrow, Resource}
import cats.implicits.toComonadOps
import cats.syntax.functor._
import cats.{Applicative, Comonad, Functor}
import derevo.derive
import derevo.tagless.applyK
import doobie.implicits._
import doobie.util.transactor.Transactor
import io.iohk.atala.prism.node.auth.model.RequestNonce
import io.iohk.atala.prism.node.identity.{PrismDid => DID}
import io.iohk.atala.prism.node.repositories.daos.RequestNoncesDAO
import io.iohk.atala.prism.node.repositories.logs.RequestNoncesRepositoryLogs
import io.iohk.atala.prism.node.utils.syntax.DBConnectionOps
import tofu.higherKind.Mid
import tofu.logging.{Logs, ServiceLogging}

@derive(applyK)
trait RequestNoncesRepository[F[_]] {
  def burn(did: DID, requestNonce: RequestNonce): F[Unit]
}

object RequestNoncesRepository {
  def apply[F[_]: MonadCancelThrow, R[_]: Functor](
      transactor: Transactor[F],
      logs: Logs[R, F]
  ): R[RequestNoncesRepository[F]] =
    for {
      serviceLogs <- logs.service[RequestNoncesRepository[F]]
    } yield {
      implicit val implicitLogs: ServiceLogging[F, RequestNoncesRepository[F]] =
        serviceLogs
      val logs: RequestNoncesRepository[Mid[F, *]] =
        new RequestNoncesRepositoryLogs[F]
      val mid = logs
      mid attach new RequestNoncesRepositoryImpl[F](transactor)
    }

  def resource[F[_]: MonadCancelThrow, R[
      _
  ]: Applicative: Functor](
      transactor: Transactor[F],
      logs: Logs[R, F]
  ): Resource[R, RequestNoncesRepository[F]] =
    Resource.eval(RequestNoncesRepository(transactor, logs))

  def unsafe[F[_]: MonadCancelThrow, R[_]: Comonad](
      transactor: Transactor[F],
      logs: Logs[R, F]
  ): RequestNoncesRepository[F] =
    RequestNoncesRepository(transactor, logs).extract
}

private final class RequestNoncesRepositoryImpl[F[_]: MonadCancelThrow](
    xa: Transactor[F]
) extends RequestNoncesRepository[F] {

  override def burn(did: DID, requestNonce: RequestNonce): F[Unit] = {
    RequestNoncesDAO
      .burn(did, requestNonce)
      .logSQLErrorsV2(s"burning, did - $did")
      .transact(xa)
  }
}
