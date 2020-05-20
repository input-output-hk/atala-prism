package io.iohk.cvp

import _root_.doobie.util.transactor.Transactor
import cats.effect.IO
import io.iohk.connector.model.ParticipantType
import io.iohk.cvp.cmanager.models.Issuer
import io.iohk.cvp.cmanager.repositories.IssuersRepository
import io.iohk.cvp.cmanager.repositories.IssuersRepository.IssuerCreationData
import io.iohk.cvp.cstore.models.Verifier
import io.iohk.cvp.cstore.repositories.VerifiersRepository
import io.iohk.cvp.cstore.repositories.VerifiersRepository.VerifierCreationData
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
      case ParticipantType.Issuer => addIssuer(Issuer.Id(id.uuid))
      case ParticipantType.Verifier => addVerifier(Verifier.Id(id.uuid))
    }
  }

  def addIssuer(id: Issuer.Id): Future[Unit] = {
    (new IssuersRepository(xa)(ec))
      .insert(IssuerCreationData(id))
      .value map (_ => ())
  }

  def addVerifier(id: Verifier.Id): Future[Unit] = {
    (new VerifiersRepository(xa))
      .insert(VerifierCreationData(id))
      .value map (_ => ())
  }
}
