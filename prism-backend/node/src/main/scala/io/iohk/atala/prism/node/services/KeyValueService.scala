package io.iohk.atala.prism.node.services

import cats.effect.IO
import io.iohk.atala.prism.node.repositories.KeyValuesRepository
import io.iohk.atala.prism.node.repositories.daos.KeyValuesDAO.KeyValue

import scala.concurrent.{ExecutionContext, Future}

class KeyValueService(keyValueRepository: KeyValuesRepository[IO])(implicit ec: ExecutionContext) {
  def get(key: String): Future[Option[String]] = {
    keyValueRepository.get(key).map(_.value).unsafeToFuture()
  }

  def getInt(key: String): Future[Option[Int]] = {
    get(key).map(_.map(_.toInt))
  }

  def set(key: String, value: Option[Any]): Future[Unit] = {
    keyValueRepository
      .upsert(KeyValue(key, value.map(_.toString)))
      .unsafeToFuture()
  }

  def setMany(keyValues: List[KeyValue]): Future[Unit] = {
    keyValueRepository
      .upsertMany(keyValues)
      .unsafeToFuture()
  }
}
