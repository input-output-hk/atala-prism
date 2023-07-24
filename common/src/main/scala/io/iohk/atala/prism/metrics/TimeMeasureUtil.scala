package io.iohk.atala.prism.metrics

import cats.data.ReaderT
import cats.effect.{IO, MonadCancel}
import cats.effect.syntax.monadCancel._
import cats.syntax.functor._
import cats.syntax.flatMap._
import cats.syntax.traverse._
import io.iohk.atala.prism.logging.TraceId.IOWithTraceIdContext
import io.iohk.atala.prism.metrics.TimeMeasureUtil.{DomainTimer, StartedDomainTimer}
import kamon.Kamon
import kamon.metric.Timer
import kamon.tag.TagSet

import scala.util.Try

trait TimeMeasureMetric[F[_]] {
  def startTimer(timer: DomainTimer): F[Try[StartedDomainTimer]]
  def stopTimer(timer: StartedDomainTimer): F[Try[Unit]]
}

object TimeMeasureMetric {
  implicit val ioTimeMeasureMetric: TimeMeasureMetric[IO] =
    new TimeMeasureMetric[IO] {
      override def startTimer(timer: DomainTimer): IO[Try[StartedDomainTimer]] =
        IO.delay(Try(StartedDomainTimer(timer.in.start())))
      override def stopTimer(timer: StartedDomainTimer): IO[Try[Unit]] =
        IO.delay(Try(timer.in.stop()))
    }
  implicit val ioWithTraceIdTimeMeasureMetric: TimeMeasureMetric[IOWithTraceIdContext] =
    new TimeMeasureMetric[IOWithTraceIdContext] {
      override def startTimer(
          timer: DomainTimer
      ): IOWithTraceIdContext[Try[StartedDomainTimer]] =
        ReaderT.liftF(ioTimeMeasureMetric.startTimer(timer))

      override def stopTimer(
          timer: StartedDomainTimer
      ): IOWithTraceIdContext[Try[Unit]] =
        ReaderT.liftF(ioTimeMeasureMetric.stopTimer(timer))
    }
}

object TimeMeasureUtil {

  private val CLIENT_REQUEST_TIMER = "client-request-time"
  private val DB_QUERY_TIMER = "db-query-time"
  private val CLIENT_TAG_NAME = "client"
  private val REPO_TAG_NAME = "repository"
  private val METHOD_TAG_NAME = "method"

  def createDBQueryTimer(
      repositoryName: String,
      methodName: String
  ): DomainTimer = {
    val tags = TagSet
      .builder()
      .add(REPO_TAG_NAME, repositoryName)
      .add(METHOD_TAG_NAME, methodName)
      .build()
    DomainTimer(Kamon.timer(DB_QUERY_TIMER).withTags(tags))
  }

  def createClientRequestTimer(
      clientName: String,
      methodName: String
  ): DomainTimer = {
    val tags = TagSet
      .builder()
      .add(CLIENT_TAG_NAME, clientName)
      .add(METHOD_TAG_NAME, methodName)
      .build()
    DomainTimer(Kamon.timer(CLIENT_REQUEST_TIMER).withTags(tags))
  }

  def measureTime[F[_], T](in: F[T], timer: DomainTimer)(implicit
      timeMeasureMetric: TimeMeasureMetric[F],
      br: MonadCancel[F, Throwable]
  ): F[T] = {
    for {
      maybeStartedTimer <- timeMeasureMetric.startTimer(timer)
      res <- in.guarantee(
        maybeStartedTimer.flatTraverse(timeMeasureMetric.stopTimer).void
      )
    } yield res
  }

  implicit class MeasureOps[F[_], T](val in: F[T]) extends AnyVal {
    def measureOperationTime(
        timer: DomainTimer
    )(implicit br: MonadCancel[F, Throwable], m: TimeMeasureMetric[F]): F[T] =
      measureTime(in, timer)
  }

  final case class DomainTimer(protected[metrics] val in: Timer) extends AnyVal
  final case class StartedDomainTimer(protected[metrics] val in: Timer.Started) extends AnyVal

}
