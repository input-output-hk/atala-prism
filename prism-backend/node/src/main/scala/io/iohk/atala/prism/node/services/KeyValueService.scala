package io.iohk.atala.prism.node.services

import cats.Functor
import cats.syntax.functor._
import io.iohk.atala.prism.node.repositories.KeyValuesRepository
import io.iohk.atala.prism.node.repositories.daos.KeyValuesDAO.KeyValue

trait KeyValueService[F[_]] {
  def get(key: String): F[Option[String]]
  def getInt(key: String): F[Option[Int]]
  def set(key: String, value: Option[Any]): F[Unit]
  def setMany(keyValues: List[KeyValue]): F[Unit]
}

object KeyValueService {
  def apply[F[_]: Functor](keyValueRepository: KeyValuesRepository[F]): KeyValueService[F] = {
    new KeyValueServiceImpl(keyValueRepository)
  }
}

private final class KeyValueServiceImpl[F[_]: Functor](keyValueRepository: KeyValuesRepository[F])
    extends KeyValueService[F] {
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
