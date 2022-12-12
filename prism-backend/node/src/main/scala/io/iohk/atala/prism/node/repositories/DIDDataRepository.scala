package io.iohk.atala.prism.node.repositories

import cats.data.EitherT
import cats.effect.{MonadCancelThrow, Resource}
import cats.syntax.either._
import cats.syntax.comonad._
import cats.syntax.functor._
import cats.{Applicative, Comonad, Functor}
import derevo.derive
import derevo.tagless.applyK
import doobie.free.connection.ConnectionIO
import doobie.implicits._
import doobie.util.transactor.Transactor
import io.iohk.atala.prism.identity.{CanonicalPrismDid => DID}
import io.iohk.atala.prism.metrics.TimeMeasureMetric
import io.iohk.atala.prism.models.DidSuffix
import io.iohk.atala.prism.node.errors.NodeError
import io.iohk.atala.prism.node.errors.NodeError.TooManyDidPublicKeysAccessAttempt
import io.iohk.atala.prism.node.models.nodeState.{
  DIDDataState,
  DIDPublicKeyState,
  DIDServiceState,
  DIDServiceWithEndpoint
}
import io.iohk.atala.prism.node.repositories.daos.{DIDDataDAO, PublicKeysDAO, ServicesDAO}
import io.iohk.atala.prism.node.repositories.logs.DIDDataRepositoryLogs
import io.iohk.atala.prism.node.repositories.metrics.DIDDataRepositoryMetrics
import io.iohk.atala.prism.utils.syntax.DBConnectionOps
import tofu.higherKind.Mid
import tofu.logging.{Logs, ServiceLogging}
import tofu.syntax.monoid.TofuSemigroupOps

@derive(applyK)
trait DIDDataRepository[F[_]] {
  def findByDid(did: DID, publicKeysLimit: Option[Int]): F[Either[NodeError, Option[DIDDataState]]]
}

object DIDDataRepository {
  def apply[F[_]: MonadCancelThrow: TimeMeasureMetric, R[_]: Functor](
      transactor: Transactor[F],
      logs: Logs[R, F]
  ): R[DIDDataRepository[F]] =
    for {
      serviceLogs <- logs.service[DIDDataRepository[F]]
    } yield {
      implicit val implicitLogs: ServiceLogging[F, DIDDataRepository[F]] =
        serviceLogs
      val metrics: DIDDataRepository[Mid[F, *]] =
        new DIDDataRepositoryMetrics[F]()
      val logs: DIDDataRepository[Mid[F, *]] = new DIDDataRepositoryLogs[F]
      val mid = metrics |+| logs
      mid attach new DIDDataRepositoryImpl[F](transactor)
    }

  def resource[F[_]: MonadCancelThrow: TimeMeasureMetric, R[
      _
  ]: Applicative: Functor](
      transactor: Transactor[F],
      logs: Logs[R, F]
  ): Resource[R, DIDDataRepository[F]] =
    Resource.eval(DIDDataRepository(transactor, logs))

  def unsafe[F[_]: MonadCancelThrow: TimeMeasureMetric, R[_]: Comonad](
      transactor: Transactor[F],
      logs: Logs[R, F]
  ): DIDDataRepository[F] = DIDDataRepository(transactor, logs).extract
}

private final class DIDDataRepositoryImpl[F[_]: MonadCancelThrow](xa: Transactor[F]) extends DIDDataRepository[F] {
  def findByDid(did: DID, publicKeysLimit: Option[Int]): F[Either[NodeError, Option[DIDDataState]]] =
    getByCanonicalSuffix(DidSuffix(did.getSuffix), publicKeysLimit)

  private def getByCanonicalSuffix(
      canonicalSuffix: DidSuffix,
      publicKeysLimit: Option[Int]
  ): F[Either[NodeError, Option[DIDDataState]]] = {

    def fetchKeys(): EitherT[ConnectionIO, NodeError, List[DIDPublicKeyState]] = publicKeysLimit match {
      case None => EitherT.liftF(PublicKeysDAO.listAllLimited(canonicalSuffix, None))
      case Some(lim) =>
        for {
          keys <- EitherT.liftF(PublicKeysDAO.listAllLimited(canonicalSuffix, Some(lim + 1)))
          _ <- EitherT.cond[ConnectionIO](
            keys.size <= lim,
            (),
            TooManyDidPublicKeysAccessAttempt(lim, None): NodeError
          )
        } yield keys
    }

    def fetchServices(): ConnectionIO[List[DIDServiceState]] = {
      ServicesDAO
        .getAllActiveByDidSuffix(canonicalSuffix)
        .map { servicesWithEndpoint =>
          servicesWithEndpoint
            .groupBy(_.serviceId)
            .map { serviceIdAndServices =>
              val (serviceId, services) = serviceIdAndServices
              /*
               * NOTE: services.head should never fail, because services will always have at least
               *       one element inside. This is guarantied by .groupBy function that is called
               *       before .map, so groupBy will create an immutable.Map of serviceId and all
               *       corresponding services, if there is none, element would not be added to the Map
               *       TODO: find a way to represent this via type system somehow
               * */
              val service = services.head

              DIDServiceState(
                serviceId = serviceId,
                id = service.id,
                didSuffix = service.didSuffix,
                `type` = service.`type`,
                serviceEndpoints = services
                  .collect { case DIDServiceWithEndpoint(_, _, _, _, Some(serviceEndpoint), _, _) =>
                    serviceEndpoint
                  }
                  .sortBy(_.urlIndex),
                addedOn = service.addedOn,
                revokedOn = service.revokedOn
              )
            }
            .toList
        }
    }

    val query = for {
      lastOperationMaybe <- EitherT.liftF(DIDDataDAO.getLastOperation(canonicalSuffix))
      keys <- fetchKeys()
      services <- EitherT(fetchServices().map(_.asRight[NodeError]))
    } yield lastOperationMaybe map { lastOperation =>
      DIDDataState(canonicalSuffix, keys, services, lastOperation)
    }

    query.value
      .logSQLErrorsV2(s"finding, did suffix - $canonicalSuffix")
      .transact(xa)
  }
}
