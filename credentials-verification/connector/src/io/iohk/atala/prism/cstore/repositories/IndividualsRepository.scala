package io.iohk.atala.prism.cstore.repositories

import cats.effect.IO
import doobie.implicits._
import doobie.util.transactor.Transactor
import io.iohk.atala.prism.connector.errors.ConnectorError
import io.iohk.atala.prism.connector.model.TokenString
import io.iohk.atala.prism.connector.repositories.daos.ConnectionTokensDAO
import io.iohk.atala.prism.console.models.{Contact, Institution}
import io.iohk.atala.prism.console.repositories.daos.ContactsDAO
import io.iohk.atala.prism.cstore.models.StoreIndividual
import io.iohk.atala.prism.cstore.repositories.daos.IndividualsDAO
import io.iohk.atala.prism.cstore.repositories.daos.IndividualsDAO.IndividualCreateData
import io.iohk.atala.prism.models.ParticipantId
import io.iohk.atala.prism.utils.FutureEither
import io.iohk.atala.prism.utils.FutureEither.FutureEitherOps

import scala.concurrent.ExecutionContext

class IndividualsRepository(xa: Transactor[IO])(implicit ec: ExecutionContext) {

  def createIndividual(
      userId: ParticipantId,
      data: IndividualCreateData
  ): FutureEither[ConnectorError, StoreIndividual] = {
    IndividualsDAO
      .insertIndividual(userId, data)
      .transact(xa)
      .unsafeToFuture()
      .map(Right(_))
      .toFutureEither
  }

  def getIndividuals(
      createdBy: ParticipantId,
      lastSeen: Option[ParticipantId],
      limit: Int
  ): FutureEither[ConnectorError, Seq[StoreIndividual]] = {
    IndividualsDAO
      .listIndividuals(createdBy, lastSeen, limit)
      .transact(xa)
      .unsafeToFuture()
      .map(Right(_))
      .toFutureEither
  }

  def generateTokenFor(
      verifierId: ParticipantId,
      contactId: Contact.Id
  ): FutureEither[ConnectorError, TokenString] = {
    val token = TokenString.random()
    val query = for {
      _ <- ConnectionTokensDAO.insert(verifierId, token)
      _ <- ContactsDAO.setConnectionToken(Institution.Id(verifierId.uuid), contactId, token)
    } yield token

    query
      .transact(xa)
      .unsafeToFuture()
      .map(Right(_))
      .toFutureEither
  }
}
