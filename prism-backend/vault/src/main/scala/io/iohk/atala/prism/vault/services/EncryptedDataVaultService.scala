package io.iohk.atala.prism.vault.services

import cats.effect.MonadThrow
import cats.syntax.apply._
import cats.syntax.flatMap._
import cats.syntax.applicativeError._
import derevo.derive
import derevo.tagless.applyK
import io.iohk.atala.prism.kotlin.crypto.SHA256Digest
import io.iohk.atala.prism.identity.DID
import io.iohk.atala.prism.logging.TraceId
import io.iohk.atala.prism.vault.model.{CreatePayload, Payload}
import io.iohk.atala.prism.vault.repositories.PayloadsRepository
import io.iohk.atala.prism.logging.GeneralLoggableInstances._
import tofu.higherKind.Mid
import tofu.logging.ServiceLogging
import tofu.syntax.logging._

@derive(applyK)
trait EncryptedDataVaultService[F[_]] {
  def storeData(
      externalId: Payload.ExternalId,
      hash: SHA256Digest,
      did: DID,
      content: Vector[Byte],
      tId: TraceId
  ): F[Payload]
  def getByPaginated(did: DID, lastSeenId: Option[Payload.Id], limit: Int, tId: TraceId): F[List[Payload]]
}

object EncryptedDataVaultService {
  def create[F[_]: MonadThrow](
      payloadsRepository: PayloadsRepository[F]
  )(implicit logs: ServiceLogging[F, EncryptedDataVaultService[F]]): EncryptedDataVaultService[F] = {
    val logging: EncryptedDataVaultService[Mid[F, *]] = new EncyptedDataVaultServiceLogging
    logging attach new EncyptedDataVaultServiceImpl(payloadsRepository)
  }
}

private final class EncyptedDataVaultServiceImpl[F[_]](payloadsRepository: PayloadsRepository[F])
    extends EncryptedDataVaultService[F] {

  override def storeData(
      externalId: Payload.ExternalId,
      hash: SHA256Digest,
      did: DID,
      content: Vector[Byte],
      tId: TraceId
  ): F[Payload] =
    payloadsRepository
      .create(
        CreatePayload(
          externalId,
          hash,
          did,
          content
        ),
        tId
      )

  override def getByPaginated(
      did: DID,
      lastSeenId: Option[Payload.Id],
      limit: Int,
      tId: TraceId
  ): F[List[Payload]] = {
    payloadsRepository
      .getByPaginated(
        did,
        lastSeenId,
        limit,
        tId
      )
  }
}

private class EncyptedDataVaultServiceLogging[F[_]: MonadThrow](implicit
    logs: ServiceLogging[F, EncryptedDataVaultService[F]]
) extends EncryptedDataVaultService[Mid[F, *]] {

  override def storeData(
      externalId: Payload.ExternalId,
      hash: SHA256Digest,
      did: DID,
      content: Vector[Byte],
      tId: TraceId
  ): Mid[F, Payload] =
    in =>
      info"storing data $externalId $did $tId" *> in
        .flatTap(p => info"storing data - successfully done ${p.id} $tId")
        .onError { e => errorCause"encountered an error while storing data! $tId" (e) }

  override def getByPaginated(
      did: DID,
      lastSeenId: Option[Payload.Id],
      limit: Int,
      tId: TraceId
  ): Mid[F, List[Payload]] =
    in =>
      info"getting paginated data $did $lastSeenId $tId" *> in
        .flatTap(p => info"getting paginated data - successfully done found ${p.size} entities $tId")
        .onError { e => errorCause"encountered an error while getting data by paginated! $tId" (e) }
}
