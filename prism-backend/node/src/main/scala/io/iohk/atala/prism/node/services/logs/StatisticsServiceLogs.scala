package io.iohk.atala.prism.node.services.logs

import cats.MonadThrow
import cats.syntax.apply._
import cats.syntax.applicativeError._
import cats.syntax.flatMap._
import io.iohk.atala.prism.node.errors
import io.iohk.atala.prism.node.models.AtalaOperationStatus
import io.iohk.atala.prism.node.services.StatisticsService
import tofu.logging.ServiceLogging
import tofu.higherKind.Mid
import tofu.syntax.logging._

class StatisticsServiceLogs[F[_]: ServiceLogging[*[_], StatisticsService[F]]: MonadThrow]
    extends StatisticsService[Mid[F, *]] {
  def getAtalaOperationsCountByStatus(status: AtalaOperationStatus): Mid[F, Either[errors.NodeError, Int]] = { in =>
    info"getting number of operations with status $status" *> in
      .flatTap(
        _.fold(
          err => error"encountered an error while getting number of operations with status $status: $err",
          _ => info"getting number of operations with status $status - successfully done"
        )
      )
      .onError(errorCause"encountered an error while getting number of operations with status $status" (_))
  }

  def retrieveMetric(metricName: String): Mid[F, Either[errors.NodeError, Int]] = { in =>
    info"retrieving metric $metricName" *> in
      .flatTap(
        _.fold(
          err => error"encountered an error while retrieving metric $metricName: $err",
          _ => info"retrieving metric $metricName - successfully done"
        )
      )
      .onError(errorCause"encountered an error while retrieving metric $metricName" (_))
  }

}
