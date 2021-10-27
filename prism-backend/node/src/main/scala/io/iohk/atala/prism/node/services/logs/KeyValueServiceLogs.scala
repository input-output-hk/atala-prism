package io.iohk.atala.prism.node.services.logs

import cats.effect.MonadThrow
import cats.syntax.apply._
import cats.syntax.applicativeError._
import cats.syntax.flatMap._
import io.iohk.atala.prism.node.repositories.daos.KeyValuesDAO
import io.iohk.atala.prism.node.services.KeyValueService
import tofu.higherKind.Mid
import tofu.logging.ServiceLogging
import tofu.syntax.logging._

private[services] class KeyValueServiceLogs[
    F[_]: ServiceLogging[*[_], KeyValueService[F]]: MonadThrow
] extends KeyValueService[Mid[F, *]] {
  override def get(key: String): Mid[F, Option[String]] =
    in =>
      info"getting by key $key" *> in
        .flatTap(res => info"getting by key - successfully done, value found - ${res.isDefined}")
        .onError(errorCause"Encountered an error while getting by key" (_))

  override def getInt(key: String): Mid[F, Option[Int]] =
    in =>
      info"getting int by key $key" *> in
        .flatTap(res => info"getting by key - successfully done, value found - ${res.isDefined}")
        .onError(errorCause"Encountered an error while getting int by key" (_))

  override def set(key: String, value: Option[Any]): Mid[F, Unit] =
    in =>
      info"setting value for the key $key with value ${value.map(_.toString)}" *> in
        .flatTap(_ => info"setting value for the key - successfully done")
        .onError(
          errorCause"Encountered an error while setting value for the key" (_)
        )

  override def setMany(keyValues: List[KeyValuesDAO.KeyValue]): Mid[F, Unit] =
    in =>
      info"setting many, size - ${keyValues.size}" *> in
        .flatTap(_ => info"setting many - successfully done")
        .onError(errorCause"Encountered an error while setting many" (_))
}
