package io.iohk.cvp

import cats.effect.IO
import doobie.implicits._
import doobie.util.transactor.Transactor
import io.iohk.connector.model.ParticipantType
import io.iohk.cvp.cmanager.models.Issuer
import io.iohk.cvp.cmanager.repositories.daos.IssuersDAO
import io.iohk.cvp.cstore.models.StoreUser
import io.iohk.cvp.cstore.repositories.daos.StoreUsersDAO
import io.iohk.cvp.models.ParticipantId

import scala.concurrent.{ExecutionContext, Future}

/**
  * This helper service is an intermediary layer that helps to propagate changes produced on the
  * connector tables, to the cmanager and cstore tables, so that they are in sync.
  */
class ParticipantPropagatorService(xa: Transactor[IO])(implicit ec: ExecutionContext) {
  def propagate(id: ParticipantId, tpe: ParticipantType, name: String, did: String): Future[Unit] = {
    tpe match {
      case ParticipantType.Holder => Future.unit // nothing to do
      case ParticipantType.Issuer => addIssuer(id = id, name = name, did = did)
      case ParticipantType.Verifier => addVerifier(id = id)
    }
  }

  def addIssuer(id: ParticipantId, name: String, did: String): Future[Unit] = {
    IssuersDAO
      .insert(Issuer(Issuer.Id(id.uuid), Issuer.Name(name), did))
      .transact(xa)
      .unsafeToFuture()
  }

  def addVerifier(id: ParticipantId): Future[Unit] = {
    StoreUsersDAO
      .insert(StoreUser(id))
      .transact(xa)
      .unsafeToFuture()
  }
}
