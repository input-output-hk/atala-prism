package io.iohk.connector.repositories

import cats.effect.IO
import doobie.implicits._
import doobie.util.transactor.Transactor
import io.iohk.connector.model.{ParticipantInfo, ParticipantLogo, ParticipantType}
import io.iohk.connector.repositories.daos.ParticipantsDAO
import io.iohk.cvp.models.ParticipantId
import io.iohk.cvp.utils.FutureEither
import io.iohk.cvp.utils.FutureEither.FutureEitherOps

import scala.concurrent.ExecutionContext

class ParticipantsRepository(xa: Transactor[IO])(implicit ec: ExecutionContext) {

  import ParticipantsRepository._

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
