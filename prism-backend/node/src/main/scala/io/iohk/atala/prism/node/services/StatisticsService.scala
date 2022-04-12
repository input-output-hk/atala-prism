package io.iohk.atala.prism.node.services

import cats.{Applicative, Comonad, Functor}
import cats.syntax.comonad._
import cats.syntax.functor._
import cats.effect.{MonadCancelThrow, Resource}
import derevo.derive
import derevo.tagless.applyK
import io.iohk.atala.prism.node.errors
import io.iohk.atala.prism.node.models.AtalaOperationStatus
import io.iohk.atala.prism.node.repositories.AtalaOperationsRepository
import io.iohk.atala.prism.node.services.logs.StatisticsServiceLogs
import tofu.higherKind.Mid
import tofu.logging.{Logs, ServiceLogging}
import kamon.Kamon

@derive(applyK)
trait StatisticsService[F[_]] {
  def getAtalaOperationsCountByStatus(status: AtalaOperationStatus): F[Either[errors.NodeError, Int]]

  def getAmountOfPendingOperations: F[Either[errors.NodeError, Int]]

  def retrieveMetric(metricName: String): F[Either[errors.NodeError, Int]]
}

private final class StatisticsServiceImpl[F[_]: Applicative](
    atalaOperationsRepository: AtalaOperationsRepository[F]
) extends StatisticsService[F] {
  def getAtalaOperationsCountByStatus(status: AtalaOperationStatus): F[Either[errors.NodeError, Int]] =
    atalaOperationsRepository.getOperationsCount(status)

  def getAmountOfPendingOperations: F[Either[errors.NodeError, Int]] = {
    atalaOperationsRepository.getOperationsCount(AtalaOperationStatus.RECEIVED)
  }

  def retrieveMetric(metricName: String): F[Either[errors.NodeError, Int]] = metricName match {
    case "amount-of-pending-operations" =>
      getAmountOfPendingOperations
    case _ =>
      Applicative[F].pure(Left(errors.NodeError.InvalidArgument(f"Metric $metricName does not exist")))
  }
}

object StatisticsService {

  final val METRICS: List[String] = List(
    "amount-of-pending-operations"
  )

  def make[I[_]: Functor, F[_]: MonadCancelThrow](
      atalaOperationsRepository: AtalaOperationsRepository[F],
      logs: Logs[I, F]
  ): I[StatisticsService[F]] = {
    for {
      serviceLogs <- logs.service[StatisticsService[F]]
    } yield {
      implicit val implicitLogs: ServiceLogging[F, StatisticsService[F]] = serviceLogs
      val logs: StatisticsService[Mid[F, *]] = new StatisticsServiceLogs[F]
      val mid: StatisticsService[Mid[F, *]] = logs

      mid attach new StatisticsServiceImpl[F](
        atalaOperationsRepository
      )
    }
  }

  def resource[I[_]: Applicative: Functor, F[_]: MonadCancelThrow](
      atalaOperationsRepository: AtalaOperationsRepository[F],
      logs: Logs[I, F]
  ): Resource[I, StatisticsService[F]] = Resource.eval(
    StatisticsService.make(atalaOperationsRepository, logs)
  )

  def unsafe[I[_]: Comonad, F[_]: MonadCancelThrow](
      atalaOperationsRepository: AtalaOperationsRepository[F],
      logs: Logs[I, F]
  ): StatisticsService[F] = StatisticsService.make(atalaOperationsRepository, logs).extract
}
