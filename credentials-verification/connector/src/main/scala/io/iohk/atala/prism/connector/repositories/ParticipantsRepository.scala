package io.iohk.atala.prism.connector.repositories

import java.util.Base64

import cats.effect.IO
import doobie.implicits._
import doobie.util.transactor.Transactor
import io.iohk.atala.prism.crypto.ECPublicKey
import io.iohk.atala.prism.connector.errors.{ConnectorError, LoggingContext, UnknownValueError, _}
import io.iohk.atala.prism.connector.model.{ParticipantInfo, ParticipantLogo, ParticipantType}
import io.iohk.atala.prism.connector.repositories.daos.ParticipantsDAO
import io.iohk.atala.prism.models.{ParticipantId, TransactionInfo}
import io.iohk.atala.prism.utils.FutureEither
import io.iohk.atala.prism.utils.FutureEither.FutureEitherOps
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
      logo = Option(request.logo),
      transactionId = Some(request.transactionInfo.transactionId),
      ledger = Some(request.transactionInfo.ledger)
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

  def findBy(publicKey: ECPublicKey): FutureEither[ConnectorError, ParticipantInfo] = {
    val encodedPublicKey = Base64.getEncoder.encodeToString(publicKey.getEncoded)
    implicit val loggingContext = LoggingContext("encodedPublicKey" -> encodedPublicKey)

    ParticipantsDAO
      .findByPublicKey(publicKey)
      .toRight(
        UnknownValueError(
          "encodedPublicKey",
          encodedPublicKey
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
      logo: ParticipantLogo,
      transactionInfo: TransactionInfo
  )

}
