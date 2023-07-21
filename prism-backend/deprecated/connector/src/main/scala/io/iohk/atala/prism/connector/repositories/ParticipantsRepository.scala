package io.iohk.atala.prism.connector.repositories

import cats.{Applicative, Comonad, Functor}
import cats.effect.Resource
import cats.syntax.applicative._
import cats.syntax.comonad._
import cats.syntax.either._
import cats.syntax.functor._
import cats.syntax.applicativeError._
import derevo.derive
import derevo.tagless.applyK
import doobie.implicits._
import doobie.util.transactor.Transactor
import io.iohk.atala.prism.connector.errors.{UnknownValueError, _}
import io.iohk.atala.prism.connector.model.{ParticipantInfo, ParticipantLogo, ParticipantType, UpdateParticipantProfile}
import io.iohk.atala.prism.connector.repositories.daos.ParticipantsDAO
import io.iohk.atala.prism.connector.repositories.logs.ParticipantsRepositoryLogs
import io.iohk.atala.prism.connector.repositories.metrics.ParticipantsRepositoryMetrics
import io.iohk.atala.prism.crypto.keys.ECPublicKey
import io.iohk.atala.prism.errors.LoggingContext
import io.iohk.atala.prism.identity.{PrismDid => DID}
import io.iohk.atala.prism.models.{AtalaOperationId, ParticipantId}
import io.iohk.atala.prism.utils.syntax.DBConnectionOps
import io.iohk.atala.prism.metrics.TimeMeasureMetric
import org.postgresql.util.PSQLException
import org.slf4j.{Logger, LoggerFactory}
import shapeless.{:+:, CNil}
import tofu.higherKind.Mid
import tofu.logging.{Logs, ServiceLogging}
import tofu.syntax.monoid.TofuSemigroupOps

import java.util.Base64
import cats.effect.MonadCancelThrow

@derive(applyK)
trait ParticipantsRepository[F[_]] {

  import io.iohk.atala.prism.connector.repositories.ParticipantsRepository._

  def create(
      request: CreateParticipantRequest
  ): F[Either[CreateParticipantError, Unit]]

  def findBy(id: ParticipantId): F[Either[FindByError, ParticipantInfo]]

  def findBy(publicKey: ECPublicKey): F[Either[FindByError, ParticipantInfo]]

  def findBy(did: DID): F[Either[FindByError, ParticipantInfo]]

  def updateParticipantProfileBy(
      id: ParticipantId,
      participantProfile: UpdateParticipantProfile
  ): F[Unit]

}

object ParticipantsRepository {

  type CreateParticipantError = InvalidRequest :+: CNil

  type FindByError = UnknownValueError :+: CNil

  def apply[F[_]: TimeMeasureMetric: MonadCancelThrow, R[_]: Functor](
      transactor: Transactor[F],
      logs: Logs[R, F]
  ): R[ParticipantsRepository[F]] =
    for {
      serviceLogs <- logs.service[ParticipantsRepository[F]]
    } yield {
      implicit val implicitLogs: ServiceLogging[F, ParticipantsRepository[F]] =
        serviceLogs
      val metrics: ParticipantsRepository[Mid[F, *]] =
        new ParticipantsRepositoryMetrics[F]
      val logs: ParticipantsRepository[Mid[F, *]] =
        new ParticipantsRepositoryLogs[F]
      val mid = metrics |+| logs
      mid attach new ParticipantsRepositoryImpl[F](transactor)
    }

  def resource[F[_]: TimeMeasureMetric: MonadCancelThrow, R[
      _
  ]: Applicative: Functor](
      transactor: Transactor[F],
      logs: Logs[R, F]
  ): Resource[R, ParticipantsRepository[F]] =
    Resource.eval(ParticipantsRepository(transactor, logs))

  def unsafe[F[_]: TimeMeasureMetric: MonadCancelThrow, R[_]: Comonad](
      transactor: Transactor[F],
      logs: Logs[R, F]
  ): ParticipantsRepository[F] =
    ParticipantsRepository(transactor, logs).extract

  case class CreateParticipantRequest(
      id: ParticipantId,
      tpe: ParticipantType,
      name: String,
      did: DID,
      logo: ParticipantLogo,
      operationId: Option[AtalaOperationId]
  )
}

private final class ParticipantsRepositoryImpl[F[_]: MonadCancelThrow](
    xa: Transactor[F]
) extends ParticipantsRepository[F]
    with ConnectorErrorSupportNew {

  import ParticipantsRepository._

  val logger: Logger = LoggerFactory.getLogger(getClass)

  def create(
      request: CreateParticipantRequest
  ): F[Either[CreateParticipantError, Unit]] = {
    val info = ParticipantInfo(
      id = request.id,
      tpe = request.tpe,
      publicKey = None,
      name = request.name,
      did = Option(request.did),
      logo = Option(request.logo),
      operationId = request.operationId
    )

    ParticipantsDAO
      .insert(info)
      .logSQLErrorsV2("inserting participants")
      .transact(xa)
      .map(_.asRight[CreateParticipantError])
      .handleErrorWith {
        case e: PSQLException if e.getServerErrorMessage.getConstraint == "participants_did_unique" =>
          Either
            .left[CreateParticipantError, Unit](
              co(InvalidRequest("DID already exists"))
            )
            .pure[F]
        case t =>
          throw t
      }
  }

  def findBy(id: ParticipantId): F[Either[FindByError, ParticipantInfo]] = {
    implicit val loggingContext = LoggingContext("id" -> id)

    ParticipantsDAO
      .findBy(id)
      .toRight[FindByError](
        co(UnknownValueError("id", id.uuid.toString).logWarnNew)
      )
      .value
      .logSQLErrorsV2(s"finding, participant id - $id")
      .transact(xa)
  }

  def findBy(
      publicKey: ECPublicKey
  ): F[Either[FindByError, ParticipantInfo]] = {
    val encodedPublicKey =
      Base64.getEncoder.encodeToString(publicKey.getEncoded)
    implicit val loggingContext = LoggingContext(
      "encodedPublicKey" -> encodedPublicKey
    )

    ParticipantsDAO
      .findByPublicKey(publicKey)
      .toRight[FindByError](
        co(
          UnknownValueError(
            "encodedPublicKey",
            encodedPublicKey
          ).logWarnNew
        )
      )
      .value
      .logSQLErrorsV2("finding by public key")
      .transact(xa)
  }

  def findBy(did: DID): F[Either[FindByError, ParticipantInfo]] = {
    implicit val loggingContext = LoggingContext("did" -> did)

    ParticipantsDAO
      .findByDID(did)
      .toRight[FindByError](
        co(UnknownValueError("did", did.getValue).logWarnNew)
      )
      .value
      .logSQLErrorsV2(s"finding, did - $did")
      .transact(xa)
  }

  def updateParticipantProfileBy(
      id: ParticipantId,
      participantProfile: UpdateParticipantProfile
  ): F[Unit] = {
    ParticipantsDAO
      .updateParticipantByID(id, participantProfile)
      .transact(xa)
  }
}
