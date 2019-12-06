package io.iohk.cvp.cmanager.repositories

import java.util.UUID

import cats.effect.IO
import doobie.implicits._
import doobie.util.transactor.Transactor
import io.iohk.connector.model.{ParticipantInfo, ParticipantLogo, ParticipantType}
import io.iohk.connector.repositories.daos.ParticipantsDAO
import io.iohk.cvp.cmanager.models.Issuer
import io.iohk.cvp.cmanager.repositories.IssuersRepository.IssuerCreationData
import io.iohk.cvp.cmanager.repositories.daos.IssuersDAO
import io.iohk.cvp.models.ParticipantId
import io.iohk.cvp.utils.FutureEither
import io.iohk.cvp.utils.FutureEither.FutureEitherOps

import scala.concurrent.ExecutionContext

object IssuersRepository {
  case class IssuerCreationData(name: Issuer.Name, did: String, logo: Option[Vector[Byte]])
}

class IssuersRepository(xa: Transactor[IO])(implicit ec: ExecutionContext) {
  def insert(data: IssuerCreationData): FutureEither[Nothing, Issuer.Id] = {
    val id = UUID.randomUUID()

    val query = for {
      _ <- IssuersDAO.insert(Issuer(Issuer.Id(id), data.name, data.did))
      _ <- ParticipantsDAO.insert(
        ParticipantInfo(
          ParticipantId(id),
          ParticipantType.Issuer,
          data.name.value,
          Some(data.did),
          data.logo.map(ParticipantLogo(_))
        )
      )
    } yield ()

    query
      .transact(xa)
      .unsafeToFuture()
      .map(_ => Right(Issuer.Id(id)))
      .toFutureEither
  }
}
