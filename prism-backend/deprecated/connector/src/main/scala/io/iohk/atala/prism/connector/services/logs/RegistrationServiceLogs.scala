package io.iohk.atala.prism.connector.services.logs

import cats.syntax.apply._
import cats.syntax.applicativeError._
import cats.syntax.flatMap._
import io.iohk.atala.prism.connector.errors.ConnectorError
import io.iohk.atala.prism.connector.model.{ParticipantLogo, ParticipantType}
import io.iohk.atala.prism.connector.services.RegistrationService
import io.iohk.atala.prism.connector.services.RegistrationService.RegisterParticipantError
import io.iohk.atala.prism.identity.PrismDid
import io.iohk.atala.prism.protos.node_models.SignedAtalaOperation
import tofu.higherKind.Mid
import tofu.logging.ServiceLogging
import tofu.syntax.logging._
import cats.MonadThrow

private[services] class RegistrationServiceLogs[
    F[_]: ServiceLogging[*[_], RegistrationService[F]]: MonadThrow
] extends RegistrationService[Mid[F, *]] {
  override def register(
      tpe: ParticipantType,
      name: String,
      logo: ParticipantLogo,
      didOrOperation: Either[PrismDid, SignedAtalaOperation]
  ): Mid[F, Either[
    RegisterParticipantError,
    RegistrationService.RegistrationResult
  ]] =
    in =>
      info"registering participant $name " *> in
        .flatTap(
          _.fold(
            er => error"Encountered an error while registering participant ${er.unify: ConnectorError}",
            res => info"registering participant - successfully done ${res.did.getSuffix}"
          )
        )
        .onError(
          errorCause"Encountered an error while registering participant" (_)
        )
}
