package io.iohk.atala.prism.node.repositories.logs

import cats.syntax.apply._
import cats.syntax.applicativeError._
import cats.syntax.flatMap._
import io.iohk.atala.prism.node.repositories.KeyValuesRepository
import io.iohk.atala.prism.node.repositories.daos.KeyValuesDAO.KeyValue
import tofu.higherKind.Mid
import tofu.logging.ServiceLogging
import tofu.syntax.logging._
import cats.MonadThrow

private[repositories] final class KeyValuesRepositoryLogs[F[_]: ServiceLogging[
  *[_],
  KeyValuesRepository[F]
]: MonadThrow]
    extends KeyValuesRepository[Mid[F, *]] {
  override def upsert(keyValue: KeyValue): Mid[F, Unit] =
    in =>
      info"upserting ${keyValue.key}" *> in
        .flatTap(_ => info"upserting - successfully done")
        .onError(errorCause"Encountered an error while upserting" (_))

  override def upsertMany(keyValues: List[KeyValue]): Mid[F, Unit] =
    in =>
      info"uperting many ${keyValues.size}" *> in
        .flatTap(_ => info"upserting many - successfully done")
        .onError(errorCause"Encountered an error while upserting many" (_))

  override def get(key: String): Mid[F, KeyValue] =
    in =>
      info"getting by key $key" *> in
        .flatTap(_ => info"getting by key - successfully done")
        .onError(errorCause"Encountered an error while getting by key" (_))
}
