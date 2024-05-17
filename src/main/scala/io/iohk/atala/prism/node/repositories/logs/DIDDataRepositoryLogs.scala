package io.iohk.atala.prism.node.repositories.logs

import cats.syntax.apply._
import cats.syntax.applicativeError._
import cats.syntax.flatMap._
import io.iohk.atala.prism.node.identity.CanonicalPrismDid
import io.iohk.atala.prism.node.errors.NodeError
import io.iohk.atala.prism.node.models.nodeState.DIDDataState
import io.iohk.atala.prism.node.repositories.DIDDataRepository
import tofu.higherKind.Mid
import tofu.logging.ServiceLogging
import tofu.syntax.logging._
import cats.MonadThrow

private[repositories] final class DIDDataRepositoryLogs[
    F[_]: ServiceLogging[*[_], DIDDataRepository[F]]: MonadThrow
] extends DIDDataRepository[Mid[F, *]] {
  override def findByDid(
      did: CanonicalPrismDid
  ): Mid[F, Either[NodeError, Option[DIDDataState]]] =
    in =>
      info"finding by did ${did.suffix}" *> in
        .flatTap(
          _.fold(
            err => error"Encountered an error while finding by did ${did.suffix}: $err",
            res => info"finding by did ${did.suffix} - successfully done, found - ${res.isDefined}"
          )
        )
        .onError(
          errorCause"Encountered an error while finding by did ${did.suffix}" (
            _
          )
        )
}
