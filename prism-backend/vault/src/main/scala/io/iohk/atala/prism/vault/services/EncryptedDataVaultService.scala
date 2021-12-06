package io.iohk.atala.prism.vault.services

import cats.effect.Resource
import cats.syntax.applicativeError._
import cats.syntax.apply._
import cats.syntax.comonad._
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.{Applicative, Comonad, Functor, MonadThrow}
import derevo.derive
import derevo.tagless.applyK
import io.iohk.atala.prism.vault.model.{CreateRecord, Record}
import io.iohk.atala.prism.vault.repositories.RecordsRepository
import tofu.higherKind.Mid
import tofu.logging.{Logs, ServiceLogging}
import tofu.syntax.logging._

@derive(applyK)
trait EncryptedDataVaultService[F[_]] {
  def storeRecord(
      type_ : Record.Type,
      id: Record.Id,
      payload: Record.Payload
  ): F[Record]

  def getRecord(
      type_ : Record.Type,
      id: Record.Id
  ): F[Option[Record]]

  def getRecordsPaginated(
      type_ : Record.Type,
      lastSeenId: Option[Record.Id],
      limit: Int
  ): F[List[Record]]

}

object EncryptedDataVaultService {
  def create[F[_]: MonadThrow, R[_]: Functor](
      recordsRepository: RecordsRepository[F],
      logs: Logs[R, F]
  ): R[EncryptedDataVaultService[F]] = {
    for {
      serviceLogs <- logs.service[EncryptedDataVaultService[F]]
    } yield {
      implicit val implicitLogs: ServiceLogging[F, EncryptedDataVaultService[F]] = serviceLogs
      val logging: EncryptedDataVaultService[Mid[F, *]] = new EncyptedDataVaultServiceLogging
      logging attach new EncyptedDataVaultServiceImpl(recordsRepository)
    }
  }
  def resource[F[_]: MonadThrow, R[_]: Applicative: Functor](
      recordsRepository: RecordsRepository[F],
      logs: Logs[R, F]
  ): Resource[R, EncryptedDataVaultService[F]] =
    Resource.eval(EncryptedDataVaultService.create(recordsRepository, logs))

  def unsafe[F[_]: MonadThrow, R[_]: Comonad](
      recordsRepository: RecordsRepository[F],
      logs: Logs[R, F]
  ): EncryptedDataVaultService[F] = EncryptedDataVaultService.create(recordsRepository, logs).extract
}

private final class EncyptedDataVaultServiceImpl[F[_]](
    recordsRepository: RecordsRepository[F]
) extends EncryptedDataVaultService[F] {

  override def storeRecord(
      type_ : Record.Type,
      id: Record.Id,
      payload: Record.Payload
  ): F[Record] =
    recordsRepository
      .create(
        CreateRecord(
          type_,
          id,
          payload
        )
      )

  override def getRecord(type_ : Record.Type, id: Record.Id): F[Option[Record]] = recordsRepository.getRecord(type_, id)

  override def getRecordsPaginated(type_ : Record.Type, lastSeenId: Option[Record.Id], limit: Int): F[List[Record]] =
    recordsRepository.getRecordsPaginated(type_, lastSeenId, limit)
}

private class EncyptedDataVaultServiceLogging[
    F[_]: MonadThrow: ServiceLogging[*[_], EncryptedDataVaultService[F]]
] extends EncryptedDataVaultService[Mid[F, *]] {

  override def storeRecord(
      type_ : Record.Type,
      id: Record.Id,
      payload: Record.Payload
  ): Mid[F, Record] =
    in =>
      info"storing encrypted record $type_ $id" *> in
        .flatTap(p => info"storing record ${p.id} - successfully done ")
        .onError { e =>
          errorCause"encountered an error while storing record!" (e)
        }

  override def getRecord(type_ : Record.Type, id: Record.Id): Mid[F, Option[Record]] =
    in =>
      info"getting a record with type = $type_, id = $id " *> in
        .flatTap(_ => info"getting a record - successfully done")
        .onError { e =>
          errorCause"encountered an error while getting a record" (e)
        }

  override def getRecordsPaginated(
      type_ : Record.Type,
      lastSeenIdOpt: Option[Record.Id],
      limit: Int
  ): Mid[F, List[Record]] =
    in =>
      info"getting paginated records type = $type_, lastId = $lastSeenIdOpt, limit = $limit " *> in
        .flatTap(p => info"getting paginated records - successfully done found ${p.size} records")
        .onError { e =>
          errorCause"encountered an error while getting records by paginated!" (e)
        }
}
