package io.iohk.atala.prism.management.console.repositories

import cats.effect.IO
import com.typesafe.config.ConfigFactory
import doobie.util.transactor.Transactor
import doobie.implicits._
import io.iohk.atala.prism.errors.LoggingContext
import io.iohk.atala.prism.identity.DID
import io.iohk.atala.prism.management.console.config.DefaultCredentialTypeConfig
import io.iohk.atala.prism.management.console.errors.{
  ManagementConsoleError,
  ManagementConsoleErrorSupport,
  UnknownValueError
}
import io.iohk.atala.prism.management.console.models.{ParticipantId, ParticipantInfo, ParticipantLogo}
import io.iohk.atala.prism.management.console.repositories.daos.{CredentialTypeDao, ParticipantsDAO}
import io.iohk.atala.prism.utils.FutureEither
import io.iohk.atala.prism.utils.FutureEither.FutureEitherOps
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.ExecutionContext

class ParticipantsRepository(
    xa: Transactor[IO],
    defaultCredentialTypeConfig: DefaultCredentialTypeConfig = DefaultCredentialTypeConfig(ConfigFactory.load())
)(implicit
    ec: ExecutionContext
) extends ManagementConsoleErrorSupport {
  import ParticipantsRepository._

  val logger: Logger = LoggerFactory.getLogger(getClass)

  def create(request: CreateParticipantRequest): FutureEither[Nothing, Unit] = {
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
      .transact(xa)
      .unsafeToFuture()
      .map(Right(_))
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
      .transact(xa)
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
}
