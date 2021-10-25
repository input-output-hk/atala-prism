package io.iohk.atala.prism.management.console.services

import cats.{Comonad, Functor, Monad}
import cats.effect.{BracketThrow, MonadThrow, Resource}
import cats.syntax.apply._
import cats.syntax.applicativeError._
import cats.syntax.comonad._
import cats.syntax.flatMap._
import cats.syntax.functor._
import derevo.derive
import derevo.tagless.applyK
import io.iohk.atala.prism.management.console.models._
import io.iohk.atala.prism.management.console.repositories.daos.ReceivedCredentialsDAO.ReceivedSignedCredentialData
import io.iohk.atala.prism.management.console.repositories.ReceivedCredentialsRepository
import io.iohk.atala.prism.metrics.TimeMeasureMetric
import tofu.higherKind.Mid
import tofu.logging.{Logs, ServiceLogging}
import tofu.syntax.logging._

@derive(applyK)
trait CredentialsStoreService[F[_]] {

  def storeCredential(data: ReceivedSignedCredentialData): F[Unit]

  def getLatestCredentialExternalId(
      participantId: ParticipantId
  ): F[Option[CredentialExternalId]]

  def getStoredCredentialsFor(
      participantId: ParticipantId,
      getStoredCredentials: GetStoredCredentials
  ): F[List[ReceivedSignedCredential]]

}

object CredentialsStoreService {

  def apply[F[_]: TimeMeasureMetric: MonadThrow, R[_]: Functor](
      receivedCredentials: ReceivedCredentialsRepository[F],
      logs: Logs[R, F]
  ): R[CredentialsStoreService[F]] =
    for {
      serviceLogs <- logs.service[CredentialsStoreService[F]]
    } yield {
      implicit val implicitLogs: ServiceLogging[F, CredentialsStoreService[F]] =
        serviceLogs
      val logs: CredentialsStoreService[Mid[F, *]] =
        new CredentialStoreServiceLogs[F]
      val mid = logs
      mid attach new CredentialsStoreServiceImpl[F](receivedCredentials)
    }

  def unsafe[F[_]: TimeMeasureMetric: BracketThrow, R[_]: Comonad](
      receivedCredentials: ReceivedCredentialsRepository[F],
      logs: Logs[R, F]
  ): CredentialsStoreService[F] =
    CredentialsStoreService(receivedCredentials, logs).extract

  def makeResource[F[_]: TimeMeasureMetric: BracketThrow, R[_]: Monad](
      receivedCredentials: ReceivedCredentialsRepository[F],
      logs: Logs[R, F]
  ): Resource[R, CredentialsStoreService[F]] =
    Resource.eval(CredentialsStoreService(receivedCredentials, logs))
}

private final class CredentialsStoreServiceImpl[F[_]](
    receivedCredentials: ReceivedCredentialsRepository[F]
) extends CredentialsStoreService[F] {
  override def storeCredential(data: ReceivedSignedCredentialData): F[Unit] =
    receivedCredentials.createReceivedCredential(data)

  override def getLatestCredentialExternalId(
      participantId: ParticipantId
  ): F[Option[CredentialExternalId]] =
    receivedCredentials.getLatestCredentialExternalId(participantId)

  override def getStoredCredentialsFor(
      participantId: ParticipantId,
      getStoredCredentials: GetStoredCredentials
  ): F[List[ReceivedSignedCredential]] =
    receivedCredentials.getCredentialsFor(
      participantId,
      getStoredCredentials.filterBy.contact
    )
}

private final class CredentialStoreServiceLogs[
    F[_]: ServiceLogging[*[_], CredentialsStoreService[F]]: MonadThrow
] extends CredentialsStoreService[Mid[F, *]] {
  override def storeCredential(
      data: ReceivedSignedCredentialData
  ): Mid[F, Unit] =
    in =>
      info"storing credential" *> in
        .flatTap(_ => info"storing credential - successfully done")
        .onError(errorCause"encountered an error while storing credential" (_))

  override def getLatestCredentialExternalId(
      participantId: ParticipantId
  ): Mid[F, Option[CredentialExternalId]] =
    in =>
      info"getting latest stored credentials $participantId" *> in
        .flatTap(_ => info"getting latest stored credentials - successfully done")
        .onError(
          errorCause"encountered an error while getting latest stored credentials" (
            _
          )
        )

  override def getStoredCredentialsFor(
      participantId: ParticipantId,
      getStoredCredentials: GetStoredCredentials
  ): Mid[F, List[ReceivedSignedCredential]] =
    in =>
      info"getting stored credentials $participantId" *> in
        .flatTap(_ => info"getting stored credentials - successfully done")
        .onError(
          errorCause"encountered an error while getting stored credentials" (_)
        )
}
