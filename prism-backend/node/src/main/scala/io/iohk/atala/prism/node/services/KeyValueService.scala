package io.iohk.atala.prism.node.services

import io.iohk.atala.prism.node.repositories.KeyValuesRepository
import io.iohk.atala.prism.node.repositories.daos.KeyValuesDAO.KeyValue

import scala.concurrent.{ExecutionContext, Future}

class KeyValueService(keyValueRepository: KeyValuesRepository)(implicit ec: ExecutionContext) {
  def get(key: String): Future[Option[String]] = {
    keyValueRepository.get(key).toFuture(_ => new RuntimeException(s"Could not get the value of key $key")).map(_.value)
  }

  def getInt(key: String): Future[Option[Int]] = {
    get(key).map(_.map(_.toInt))
  }

  def set(key: String, value: Option[Any]): Future[Unit] = {
    keyValueRepository
      .upsert(KeyValue(key, value.map(_.toString)))
      .toFuture(_ => new RuntimeException(s"Could not set key $key to value $value"))
  }

  def setMany(keyValues: List[KeyValue]): Future[List[Unit]] = {
    keyValueRepository
      .upsertMany(keyValues)
      .toFuture(_ =>
        new RuntimeException(s"Could not set keys [${keyValues.map(_.key)}] to values [${keyValues.map(_.value)}]")
      )
  }
}
