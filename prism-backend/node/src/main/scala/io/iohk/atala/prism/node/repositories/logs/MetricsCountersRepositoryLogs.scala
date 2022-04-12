package io.iohk.atala.prism.node.repositories.logs

import cats.MonadThrow
import cats.syntax.apply._
import cats.syntax.applicativeError._
import cats.syntax.flatMap._
import io.iohk.atala.prism.node.repositories.MetricsCountersRepository
import tofu.higherKind.Mid
import tofu.syntax.logging._
import tofu.logging.ServiceLogging

private[repositories] final class MetricsCountersRepositoryLogs[F[_]: ServiceLogging[
  *[_],
  MetricsCountersRepository[F]
]: MonadThrow]
    extends MetricsCountersRepository[Mid[F, *]] {

  /** Gets the counter by metric name
    */
  override def getCounter(counterName: String): Mid[F, Int] =
    in =>
      info"getting counter $counterName" *> in
        .flatTap(_ => info"getting counter $counterName - successfully done")
        .onError(errorCause"Encountered an error while getting counter $counterName" (_))
}
