package io.iohk.connector.repositories

import java.util.Base64

import cats.effect.IO
import doobie.implicits._
import doobie.util.transactor.Transactor
import io.iohk.connector.errors.{ConnectorError, LoggingContext, UnknownValueError, _}
import io.iohk.connector.model.{ParticipantInfo, ParticipantLogo, ParticipantType}
import io.iohk.connector.repositories.daos.ParticipantsDAO
import io.iohk.cvp.crypto.ECKeys.EncodedPublicKey
import io.iohk.cvp.models.ParticipantId
import io.iohk.cvp.utils.FutureEither
import io.iohk.cvp.utils.FutureEither.FutureEitherOps
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.ExecutionContext

class ParticipantsRepository(xa: Transactor[IO])(implicit ec: ExecutionContext) extends ErrorSupport {

  import ParticipantsRepository._

  val logger: Logger = LoggerFactory.getLogger(getClass)

  def create(request: CreateParticipantRequest): FutureEither[Nothing, Unit] = {
    val info = ParticipantInfo(
      id = request.id,
      tpe = request.tpe,
      publicKey = None,
      name = request.name,
      did = Option(request.did),
      logo = Option(request.logo)
    )

    ParticipantsDAO
      .insert(info)
      .transact(xa)
      .unsafeToFuture()
      .map(Right(_))
      .toFutureEither
  }

  def findBy(id: ParticipantId): FutureEither[ConnectorError, ParticipantInfo] = {
    implicit val loggingContext = LoggingContext("id" -> id)

    ParticipantsDAO
      .findBy(id)
      .toRight(
        UnknownValueError("id", id.uuid.toString).logWarn
      )
      .transact(xa)
      .value
      .unsafeToFuture()
      .toFutureEither
  }

  def findBy(encodedPublicKey: EncodedPublicKey): FutureEither[ConnectorError, ParticipantInfo] = {
    implicit val loggingContext = LoggingContext("encodedPublicKey" -> encodedPublicKey)

    ParticipantsDAO
      .findByPublicKey(encodedPublicKey)
      .toRight(
        UnknownValueError(
          "encodedPublicKey",
          Base64.getEncoder.encodeToString(encodedPublicKey.bytes.toArray)
        ).logWarn
      )
      .transact(xa)
      .value
      .unsafeToFuture()
      .toFutureEither
  }

  def findBy(did: String): FutureEither[ConnectorError, ParticipantInfo] = {
    implicit val loggingContext = LoggingContext("did" -> did)

    ParticipantsDAO
      .findByDID(did)
      .toRight(
        UnknownValueError("did", did).logWarn
      )
      .transact(xa)
      .value
      .unsafeToFuture()
      .toFutureEither
  }

}

object ParticipantsRepository {
  case class CreateParticipantRequest(
      id: ParticipantId,
      tpe: ParticipantType,
      name: String,
      did: String,
      logo: ParticipantLogo
  )

}
