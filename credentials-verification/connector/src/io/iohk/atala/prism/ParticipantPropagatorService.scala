package io.iohk.atala.prism

import _root_.doobie.util.transactor.Transactor
import cats.effect.IO
import io.iohk.connector.model.ParticipantType
import io.iohk.atala.prism.cmanager.models.Issuer
import io.iohk.atala.prism.cmanager.repositories.IssuersRepository
import io.iohk.atala.prism.cmanager.repositories.IssuersRepository.IssuerCreationData
import io.iohk.atala.prism.cstore.models.Verifier
import io.iohk.atala.prism.cstore.repositories.VerifiersRepository
import io.iohk.atala.prism.cstore.repositories.VerifiersRepository.VerifierCreationData
import io.iohk.atala.prism.models.ParticipantId

import scala.concurrent.{ExecutionContext, Future}

/**
  * This helper service is an intermediary layer that helps to propagate changes produced on the
  * connector tables, to the cmanager and cstore tables, so that they are in sync.
  */
class ParticipantPropagatorService(xa: Transactor[IO])(implicit ec: ExecutionContext) {
  def propagate(id: ParticipantId, tpe: ParticipantType): Future[Unit] = {
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
