package io.iohk.atala.prism.node.services

import io.iohk.atala.prism.logging.TraceId
import io.iohk.atala.prism.logging.TraceId.IOWithTraceIdContext
import io.iohk.atala.prism.node.repositories.KeyValuesRepository
import io.iohk.atala.prism.node.repositories.daos.KeyValuesDAO.KeyValue

import scala.concurrent.{ExecutionContext, Future}

class KeyValueService(keyValueRepository: KeyValuesRepository[IOWithTraceIdContext])(implicit ec: ExecutionContext) {
  def get(key: String): Future[Option[String]] = {
    keyValueRepository.get(key).map(_.value).run(TraceId.generateYOLO).unsafeToFuture()
  }

  def getInt(key: String): Future[Option[Int]] = {
    get(key).map(_.map(_.toInt))
  }

  def set(key: String, value: Option[Any]): Future[Unit] = {
    keyValueRepository
      .upsert(KeyValue(key, value.map(_.toString)))
      .run(TraceId.generateYOLO)
      .unsafeToFuture()
  }

  def setMany(keyValues: List[KeyValue]): Future[Unit] = {
    keyValueRepository
      .upsertMany(keyValues)
      .run(TraceId.generateYOLO)
      .unsafeToFuture()
  }
}
