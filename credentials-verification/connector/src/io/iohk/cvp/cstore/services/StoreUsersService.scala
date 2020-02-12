package io.iohk.cvp.cstore.services

import cats.effect.IO
import doobie.implicits._
import doobie.util.transactor.Transactor
import io.iohk.connector.model.{ParticipantInfo, ParticipantLogo, ParticipantType}
import io.iohk.connector.repositories.daos.ParticipantsDAO
import io.iohk.cvp.cstore.models.StoreUser
import io.iohk.cvp.cstore.repositories.daos.StoreUsersDAO
import io.iohk.cvp.cstore.services.StoreUsersService.StoreUserCreationData
import io.iohk.cvp.models.ParticipantId
import io.iohk.cvp.utils.FutureEither
import io.iohk.cvp.utils.FutureEither.FutureEitherOps

import scala.concurrent.ExecutionContext

object StoreUsersService {
  case class StoreUserCreationData(name: String, logo: Option[Vector[Byte]])
}

class StoreUsersService(xa: Transactor[IO])(implicit ec: ExecutionContext) {
  def insert(data: StoreUserCreationData): FutureEither[Nothing, ParticipantId] = {
    val id = ParticipantId.random()

    val query = for {
      _ <- StoreUsersDAO.insert(StoreUser(id))
      _ <- ParticipantsDAO.insert(
        ParticipantInfo(id, ParticipantType.Verifier, None, data.name, None, data.logo.map(ParticipantLogo))
      )
    } yield ()

    query
      .transact(xa)
      .unsafeToFuture()
      .map(_ => Right(id))
      .toFutureEither
  }
}
