package io.iohk.atala.prism.connector.repositories

import cats.effect.IO
import cats.syntax.applicative._
import cats.syntax.either._
import doobie.implicits._
import doobie.util.transactor.Transactor
import io.iohk.atala.prism.connector.errors.{ConnectorError, UnknownValueError, _}
import io.iohk.atala.prism.connector.model.{ParticipantInfo, ParticipantLogo, UpdateParticipantProfile, ParticipantType}
import io.iohk.atala.prism.connector.repositories.daos.ParticipantsDAO
import io.iohk.atala.prism.crypto.ECPublicKey
import io.iohk.atala.prism.errors.LoggingContext
import io.iohk.atala.prism.identity.DID
import io.iohk.atala.prism.models.{ParticipantId, TransactionInfo}
import io.iohk.atala.prism.utils.FutureEither
import io.iohk.atala.prism.utils.FutureEither.FutureEitherOps
import org.postgresql.util.PSQLException
import org.slf4j.{Logger, LoggerFactory}

import java.util.Base64

class ParticipantsRepository(xa: Transactor[IO]) extends ConnectorErrorSupport {

  import ParticipantsRepository._

  val logger: Logger = LoggerFactory.getLogger(getClass)

  def create(request: CreateParticipantRequest): FutureEither[ConnectorError, Unit] = {
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
      .map(_.asRight)
      .handleErrorWith {
        case e: PSQLException if e.getServerErrorMessage.getConstraint == "participants_did_unique" =>
          InvalidRequest("DID already exists").asLeft.pure[IO]
      }
      .unsafeToFuture()
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

  def findBy(did: DID): FutureEither[ConnectorError, ParticipantInfo] = {
    implicit val loggingContext = LoggingContext("did" -> did)

    ParticipantsDAO
      .findByDID(did)
      .toRight(
        UnknownValueError("did", did.value).logWarn
      )
      .transact(xa)
      .value
      .unsafeToFuture()
      .toFutureEither
  }

  def updateParticipantProfileBy(
      id: ParticipantId,
      participantProfile: UpdateParticipantProfile
  ): FutureEither[ConnectorError, Unit] = {
    ParticipantsDAO
      .updateParticipantByID(id, participantProfile)
      .transact(xa)
      .map(_.asRight)
      .unsafeToFuture()
      .toFutureEither
  }
}

object ParticipantsRepository {
  case class CreateParticipantRequest(
      id: ParticipantId,
      tpe: ParticipantType,
      name: String,
      did: DID,
      logo: ParticipantLogo,
      transactionInfo: TransactionInfo
  )

}
