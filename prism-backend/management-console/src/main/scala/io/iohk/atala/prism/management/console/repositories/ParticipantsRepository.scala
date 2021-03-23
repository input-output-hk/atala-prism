package io.iohk.atala.prism.management.console.repositories

import cats.effect.IO
import cats.syntax.applicative._
import cats.syntax.either._
import com.typesafe.config.ConfigFactory
import doobie.implicits._
import doobie.util.transactor.Transactor
import io.iohk.atala.prism.errors.LoggingContext
import io.iohk.atala.prism.identity.DID
import io.iohk.atala.prism.management.console.config.DefaultCredentialTypeConfig
import io.iohk.atala.prism.management.console.errors.{
  InvalidRequest,
  ManagementConsoleError,
  ManagementConsoleErrorSupport,
  UnknownValueError
}
import io.iohk.atala.prism.management.console.models.{
  ParticipantId,
  ParticipantInfo,
  ParticipantLogo,
  UpdateParticipantProfile
}
import io.iohk.atala.prism.management.console.repositories.daos.{CredentialTypeDao, ParticipantsDAO}
import io.iohk.atala.prism.utils.FutureEither
import io.iohk.atala.prism.utils.FutureEither.FutureEitherOps
import io.iohk.atala.prism.utils.syntax.DBConnectionOps
import org.postgresql.util.PSQLException
import org.slf4j.{Logger, LoggerFactory}

class ParticipantsRepository(
    xa: Transactor[IO],
    defaultCredentialTypeConfig: DefaultCredentialTypeConfig = DefaultCredentialTypeConfig(ConfigFactory.load())
) extends ManagementConsoleErrorSupport {
  import ParticipantsRepository._

  val logger: Logger = LoggerFactory.getLogger(getClass)

  def create(request: CreateParticipantRequest): FutureEither[ManagementConsoleError, Unit] = {
    val info = ParticipantInfo(
      id = request.id,
      name = request.name,
      did = request.did,
      logo = Option(request.logo)
    )

    (for {
      _ <- ParticipantsDAO.insert(info)
      _ <- CredentialTypeDao.insertDefaultCredentialTypes(request.id, defaultCredentialTypeConfig)
    } yield ())
      .logSQLErrors("creating", logger)
      .transact(xa)
      .map(_.asRight)
      .handleErrorWith {
        case e: PSQLException if e.getServerErrorMessage.getConstraint == "participants_did_unique" =>
          InvalidRequest("DID already exists").asLeft.pure[IO]
      }
      .unsafeToFuture()
      .toFutureEither
  }

  def findBy(id: ParticipantId): FutureEither[ManagementConsoleError, ParticipantInfo] = {
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
      .unsafeToFuture()
      .toFutureEither
  }

  def findBy(did: DID): FutureEither[ManagementConsoleError, ParticipantInfo] = {
    implicit val loggingContext = LoggingContext("did" -> did)

    ParticipantsDAO
      .findByDID(did)
      .map(
        _.toRight(
          UnknownValueError("did", did.value).logWarn
        )
      )
      .logSQLErrors(s"finding, did - $did", logger)
      .transact(xa)
      .unsafeToFuture()
      .toFutureEither
  }

  def update(request: UpdateParticipantProfileRequest): FutureEither[ManagementConsoleError, Unit] = {
    ParticipantsDAO
      .updateParticipantByID(request.id, request.participantProfile)
      .transact(xa)
      .map(_.asRight)
      .unsafeToFuture()
      .toFutureEither
  }
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
}
