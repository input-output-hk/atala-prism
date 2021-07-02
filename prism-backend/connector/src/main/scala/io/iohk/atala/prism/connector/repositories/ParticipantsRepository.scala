package io.iohk.atala.prism.connector.repositories

import cats.effect.{Bracket, BracketThrow}
import cats.syntax.applicative._
import cats.syntax.either._
import cats.syntax.functor._
import cats.syntax.applicativeError._
import derevo.derive
import derevo.tagless.applyK
import doobie.implicits._
import doobie.util.transactor.Transactor
import io.iohk.atala.prism.connector.AtalaOperationId
import io.iohk.atala.prism.connector.errors.{ConnectorError, UnknownValueError, _}
import io.iohk.atala.prism.connector.model.{ParticipantInfo, ParticipantLogo, ParticipantType, UpdateParticipantProfile}
import io.iohk.atala.prism.connector.repositories.ParticipantsRepository.CreateParticipantRequest
import io.iohk.atala.prism.connector.repositories.daos.ParticipantsDAO
import io.iohk.atala.prism.crypto.ECPublicKey
import io.iohk.atala.prism.errors.LoggingContext
import io.iohk.atala.prism.identity.DID
import io.iohk.atala.prism.models.ParticipantId
import io.iohk.atala.prism.metrics.TimeMeasureUtil.MeasureOps
import io.iohk.atala.prism.utils.syntax.DBConnectionOps
import io.iohk.atala.prism.metrics.{TimeMeasureMetric, TimeMeasureUtil}
import org.postgresql.util.PSQLException
import org.slf4j.{Logger, LoggerFactory}
import tofu.higherKind.Mid

import java.util.Base64

@derive(applyK)
trait ParticipantsRepository[F[_]] {

  def create(request: CreateParticipantRequest): F[Either[ConnectorError, Unit]]

  def findBy(id: ParticipantId): F[Either[ConnectorError, ParticipantInfo]]

  def findBy(publicKey: ECPublicKey): F[Either[ConnectorError, ParticipantInfo]]

  def findBy(did: DID): F[Either[ConnectorError, ParticipantInfo]]

  def updateParticipantProfileBy(
      id: ParticipantId,
      participantProfile: UpdateParticipantProfile
  ): F[Unit]

}

object ParticipantsRepository {

  def apply[F[_]: TimeMeasureMetric](
      transactor: Transactor[F]
  )(implicit br: Bracket[F, Throwable]): ParticipantsRepository[F] = {
    val metrics: ParticipantsRepository[Mid[F, *]] = new ParticipantsRepositoryMetrics[F]
    metrics attach new ParticipantsRepositoryImpl[F](transactor)
  }

  case class CreateParticipantRequest(
      id: ParticipantId,
      tpe: ParticipantType,
      name: String,
      did: DID,
      logo: ParticipantLogo,
      operationId: Option[AtalaOperationId]
  )
}

private final class ParticipantsRepositoryImpl[F[_]: BracketThrow](xa: Transactor[F])
    extends ParticipantsRepository[F]
    with ConnectorErrorSupport {

  import ParticipantsRepository._

  val logger: Logger = LoggerFactory.getLogger(getClass)

  def create(request: CreateParticipantRequest): F[Either[ConnectorError, Unit]] = {
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
      .logSQLErrors("inserting participants", logger)
      .transact(xa)
      .map(_.asRight[ConnectorError])
      .handleErrorWith {
        case e: PSQLException if e.getServerErrorMessage.getConstraint == "participants_did_unique" =>
          Either.left[ConnectorError, Unit](InvalidRequest("DID already exists")).pure[F]
      }
  }

  def findBy(id: ParticipantId): F[Either[ConnectorError, ParticipantInfo]] = {
    implicit val loggingContext = LoggingContext("id" -> id)

    ParticipantsDAO
      .findBy(id)
      .toRight(
        UnknownValueError("id", id.uuid.toString).logWarn
      )
      .value
      .logSQLErrors(s"finding, participant id - $id", logger)
      .transact(xa)
  }

  def findBy(publicKey: ECPublicKey): F[Either[ConnectorError, ParticipantInfo]] = {
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
      .value
      .logSQLErrors("finding by public key", logger)
      .transact(xa)
  }

  def findBy(did: DID): F[Either[ConnectorError, ParticipantInfo]] = {
    implicit val loggingContext = LoggingContext("did" -> did)

    ParticipantsDAO
      .findByDID(did)
      .toRight(
        UnknownValueError("did", did.value).logWarn
      )
      .value
      .logSQLErrors(s"finding, did - $did", logger)
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

private final class ParticipantsRepositoryMetrics[F[_]: TimeMeasureMetric](implicit br: Bracket[F, Throwable])
    extends ParticipantsRepository[Mid[F, *]] {

  private val repoName = "ParticipantsRepository"
  private lazy val createTimer = TimeMeasureUtil.createDBQueryTimer(repoName, "create")
  private lazy val findByIdTimer = TimeMeasureUtil.createDBQueryTimer(repoName, "findById")
  private lazy val findByPublicKeyTimer = TimeMeasureUtil.createDBQueryTimer(repoName, "findByPublicKey")
  private lazy val findByDidTimer = TimeMeasureUtil.createDBQueryTimer(repoName, "findByDid")
  private lazy val updateParticipantProfileByTimer =
    TimeMeasureUtil.createDBQueryTimer(repoName, "updateParticipantProfileBy")

  override def create(request: CreateParticipantRequest): Mid[F, Either[ConnectorError, Unit]] =
    _.measureOperationTime(createTimer)

  override def findBy(id: ParticipantId): Mid[F, Either[ConnectorError, ParticipantInfo]] =
    _.measureOperationTime(findByIdTimer)

  override def findBy(publicKey: ECPublicKey): Mid[F, Either[ConnectorError, ParticipantInfo]] =
    _.measureOperationTime(findByPublicKeyTimer)

  override def findBy(did: DID): Mid[F, Either[ConnectorError, ParticipantInfo]] =
    _.measureOperationTime(findByDidTimer)

  override def updateParticipantProfileBy(
      id: ParticipantId,
      participantProfile: UpdateParticipantProfile
  ): Mid[F, Unit] = _.measureOperationTime(updateParticipantProfileByTimer)
}
