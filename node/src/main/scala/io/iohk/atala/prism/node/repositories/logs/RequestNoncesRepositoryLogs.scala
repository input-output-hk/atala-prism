package io.iohk.atala.prism.node.repositories.logs

import cats.MonadThrow
import cats.syntax.applicativeError._
import cats.syntax.apply._
import cats.syntax.flatMap._
import io.iohk.atala.prism.node.auth.model
import io.iohk.atala.prism.node.identity.{PrismDid => DID}
import io.iohk.atala.prism.node.repositories.RequestNoncesRepository
import tofu.higherKind.Mid
import tofu.logging.ServiceLogging
import tofu.syntax.logging._

private[repositories] class RequestNoncesRepositoryLogs[
    F[_]: ServiceLogging[*[_], RequestNoncesRepository[F]]
](implicit
    monadThrow: MonadThrow[F]
) extends RequestNoncesRepository[Mid[F, *]] {

  override def burn(did: DID, requestNonce: model.RequestNonce): Mid[F, Unit] =
    in =>
      info"burning nonce ${did.suffix}" *> in
        .flatMap(_ => info"burning nonce - successfully done")
        .onError(errorCause"encountered an error while burning nonce" (_))
}
