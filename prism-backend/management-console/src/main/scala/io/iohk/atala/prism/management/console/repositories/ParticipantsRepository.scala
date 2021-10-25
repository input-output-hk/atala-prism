package io.iohk.atala.prism.management.console.repositories

import cats.{Comonad, Functor, Monad}
import cats.effect.{BracketThrow, Resource}
import cats.syntax.applicative._
import cats.syntax.applicativeError._
import cats.syntax.comonad._
import cats.syntax.either._
import cats.syntax.functor._
import com.typesafe.config.ConfigFactory
import derevo.tagless.applyK
import derevo.derive
import doobie.implicits._
import doobie.util.transactor.Transactor
import io.iohk.atala.prism.errors.LoggingContext
import io.iohk.atala.prism.identity.{PrismDid => DID}
import io.iohk.atala.prism.management.console.config.DefaultCredentialTypeConfig
import io.iohk.atala.prism.management.console.errors._
import io.iohk.atala.prism.management.console.models._
import io.iohk.atala.prism.management.console.repositories.ParticipantsRepository.{
  CreateParticipantRequest,
  UpdateParticipantProfileRequest
}
import io.iohk.atala.prism.management.console.repositories.daos.{CredentialTypeDao, ParticipantsDAO}
import io.iohk.atala.prism.management.console.repositories.logs.ParticipantsRepositoryLogs
import io.iohk.atala.prism.management.console.repositories.metrics.ParticipantsRepositoryMetrics
import io.iohk.atala.prism.metrics.TimeMeasureMetric
import io.iohk.atala.prism.utils.syntax.DBConnectionOps
import org.postgresql.util.PSQLException
import org.slf4j.{Logger, LoggerFactory}
import tofu.higherKind.Mid
import tofu.logging.{Logs, ServiceLogging}
import tofu.syntax.monoid.TofuSemigroupOps

@derive(applyK)
trait ParticipantsRepository[F[_]] {

  def create(
      request: CreateParticipantRequest
  ): F[Either[ManagementConsoleError, Unit]]

  def findBy(
      id: ParticipantId
  ): F[Either[ManagementConsoleError, ParticipantInfo]]

  def findBy(did: DID): F[Either[ManagementConsoleError, ParticipantInfo]]

  def update(request: UpdateParticipantProfileRequest): F[Unit]

}

object ParticipantsRepository {

  final case class CreateParticipantRequest(
      id: ParticipantId,
      name: String,
      did: DID,
      logo: ParticipantLogo
  )

  final case class UpdateParticipantProfileRequest(
      id: ParticipantId,
      participantProfile: UpdateParticipantProfile
  )

  def apply[F[_]: TimeMeasureMetric: BracketThrow, R[_]: Functor](
      transactor: Transactor[F],
      logs: Logs[R, F],
      defaultCredentialTypeConfig: DefaultCredentialTypeConfig
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
      mid attach new ParticipantsRepositoryImpl[F](
        transactor,
        defaultCredentialTypeConfig
      )
    }

  def unsafe[F[_]: TimeMeasureMetric: BracketThrow, R[_]: Comonad](
      transactor: Transactor[F],
      logs: Logs[R, F],
      defaultCredentialTypeConfig: DefaultCredentialTypeConfig = DefaultCredentialTypeConfig(ConfigFactory.load())
  ): ParticipantsRepository[F] = ParticipantsRepository(
    transactor,
    logs,
    defaultCredentialTypeConfig
  ).extract

  def makeResource[F[_]: TimeMeasureMetric: BracketThrow, R[_]: Monad](
      transactor: Transactor[F],
      logs: Logs[R, F],
      defaultCredentialTypeConfig: DefaultCredentialTypeConfig = DefaultCredentialTypeConfig(ConfigFactory.load())
  ): Resource[R, ParticipantsRepository[F]] =
    Resource.eval(
      ParticipantsRepository(transactor, logs, defaultCredentialTypeConfig)
    )

}

private final class ParticipantsRepositoryImpl[F[_]: BracketThrow](
    xa: Transactor[F],
    defaultCredentialTypeConfig: DefaultCredentialTypeConfig
) extends ParticipantsRepository[F]
    with ManagementConsoleErrorSupport {

  val logger: Logger = LoggerFactory.getLogger(getClass)

  def create(
      request: CreateParticipantRequest
  ): F[Either[ManagementConsoleError, Unit]] = {
    val info = ParticipantInfo(
      id = request.id,
      name = request.name,
      did = request.did,
      logo = Option(request.logo)
    )

    (for {
      _ <- ParticipantsDAO.insert(info)
      _ <- CredentialTypeDao.insertDefaultCredentialTypes(
        request.id,
        defaultCredentialTypeConfig
      )
    } yield ())
      .logSQLErrors("creating", logger)
      .transact(xa)
      .map(_.asRight[ManagementConsoleError])
      .handleErrorWith {
        case e: PSQLException if e.getServerErrorMessage.getConstraint == "participants_did_unique" =>
          Either
            .left[ManagementConsoleError, Unit](
              InvalidRequest("DID already exists")
            )
            .pure[F]
        case t =>
          throw t
      }
  }

  def findBy(
      id: ParticipantId
  ): F[Either[ManagementConsoleError, ParticipantInfo]] = {
    implicit val loggingContext = LoggingContext("id" -> id)

    ParticipantsDAO
      .findBy(id)
      .map(
        _.toRight(
          UnknownValueError("id", id.uuid.toString).logWarn
        )
      )
      .logSQLErrors(s"finding, participant id - $id", logger)
      .transact(xa)
  }

  def findBy(did: DID): F[Either[ManagementConsoleError, ParticipantInfo]] = {
    implicit val loggingContext = LoggingContext("did" -> did)

    ParticipantsDAO
      .findByDID(did)
      .map(
        _.toRight(
          UnknownValueError("did", did.getValue).logWarn
        )
      )
      .logSQLErrors(s"finding, did - $did", logger)
      .transact(xa)
  }

  def update(request: UpdateParticipantProfileRequest): F[Unit] =
    ParticipantsDAO
      .updateParticipantByID(request.id, request.participantProfile)
      .transact(xa)
}
