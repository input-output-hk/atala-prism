package io.iohk.atala.prism.node.services

import cats.effect.Resource
import cats.{Applicative, Comonad, Functor}
import cats.syntax.comonad._
import cats.syntax.functor._
import derevo.derive
import derevo.tagless.applyK
import io.iohk.atala.prism.node.repositories.KeyValuesRepository
import io.iohk.atala.prism.node.repositories.daos.KeyValuesDAO.KeyValue
import io.iohk.atala.prism.node.services.logs.KeyValueServiceLogs
import tofu.higherKind.Mid
import tofu.logging.{Logs, ServiceLogging}
import cats.MonadThrow

@derive(applyK)
trait KeyValueService[F[_]] {
  def get(key: String): F[Option[String]]
  def getInt(key: String): F[Option[Int]]
  def set(key: String, value: Option[Any]): F[Unit]
  def setMany(keyValues: List[KeyValue]): F[Unit]
}

object KeyValueService {
  def apply[F[_]: MonadThrow, R[_]: Functor](
      keyValueRepository: KeyValuesRepository[F],
      logs: Logs[R, F]
  ): R[KeyValueService[F]] =
    for {
      serviceLogs <- logs.service[KeyValueService[F]]
    } yield {
      implicit val implicitLogs: ServiceLogging[F, KeyValueService[F]] =
        serviceLogs
      val logs: KeyValueService[Mid[F, *]] = new KeyValueServiceLogs[F]
      val mid = logs
      mid attach new KeyValueServiceImpl[F](keyValueRepository)
    }

  def resource[F[_]: MonadThrow, R[_]: Applicative: Functor](
      keyValueRepository: KeyValuesRepository[F],
      logs: Logs[R, F]
  ): Resource[R, KeyValueService[F]] =
    Resource.eval(KeyValueService(keyValueRepository, logs))

  def unsafe[F[_]: MonadThrow, R[_]: Comonad](
      keyValueRepository: KeyValuesRepository[F],
      logs: Logs[R, F]
  ): KeyValueService[F] = KeyValueService(keyValueRepository, logs).extract
}

private final class KeyValueServiceImpl[F[_]: Functor](
    keyValueRepository: KeyValuesRepository[F]
) extends KeyValueService[F] {
  def get(key: String): F[Option[String]] =
    keyValueRepository.get(key).map(_.value)

  def getInt(key: String): F[Option[Int]] =
    get(key).map(_.map(_.toInt))

  def set(key: String, value: Option[Any]): F[Unit] =
    keyValueRepository
      .upsert(KeyValue(key, value.map(_.toString)))

  def setMany(keyValues: List[KeyValue]): F[Unit] =
    keyValueRepository
      .upsertMany(keyValues)
}
