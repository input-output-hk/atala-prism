package io.iohk.atala.prism.node.repositories.logs

import cats.implicits.catsSyntaxApplicativeError
import cats.syntax.apply._
import cats.syntax.flatMap._
import io.iohk.atala.prism.node.models
import io.iohk.atala.prism.node.operations.protocolVersion.SUPPORTED_VERSION
import io.iohk.atala.prism.node.repositories.ProtocolVersionRepository
import tofu.higherKind.Mid
import tofu.logging.ServiceLogging
import tofu.syntax.logging._
import cats.MonadThrow

private[repositories] final class ProtocolVersionRepositoryLogs[F[
    _
]: MonadThrow: ServiceLogging[*[
  _
], ProtocolVersionRepository[F]]]
    extends ProtocolVersionRepository[Mid[F, *]] {

  override def ifNodeSupportsCurrentProtocol(): Mid[F, Either[models.ProtocolVersion, Unit]] =
    in =>
      info"checking if a node supports currently effective protocol version" *> in
        .flatTap(
          _.fold(
            pv =>
              error"Node doesn't support current protocol version, which is: $pv. Node supports: $SUPPORTED_VERSION",
            _ => info"Node supports current protocol version"
          )
        )
        .onError(
          errorCause"Encountered an error while checking if node supports currently effective protocol version" (
            _
          )
        )

  override def markEffective(
      blockLevel: Int
  ): Mid[F, Option[models.ProtocolVersionInfo]] =
    in =>
      debug"marking effective protocol versions that get effective after or at block level $blockLevel" *> in
        .flatTap(
          _.fold(debug"No protocol versions to mark effective")(pv =>
            info"Protocol version ${pv.versionName} ${pv.protocolVersion} turned effective"
          )
        )
        .onError(
          errorCause"Encountered an error while marking protocol versions effective" (
            _
          )
        )
}
