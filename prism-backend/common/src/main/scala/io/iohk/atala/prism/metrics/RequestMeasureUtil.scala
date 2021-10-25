package io.iohk.atala.prism.metrics

import cats.syntax.apply._
import cats.syntax.flatMap._
import cats.instances.future._
import kamon.Kamon
import kamon.metric.{Counter, Gauge, Metric, Timer}
import kamon.tag.TagSet
import org.slf4j.LoggerFactory

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Try}
import scala.util.control.NonFatal

object RequestMeasureUtil {

  private val logger = LoggerFactory.getLogger(this.getClass)
  private val REQUEST_TIME_METRIC_NAME = "request-time"
  private val ACTIVE_REQUESTS_METRIC_NAME = "active-requests"
  private val ERROR_METRIC_NAME = "error-count"
  // Should be lazy, otherwise scala.UninitializedFieldError will be encountered
  private lazy val requestTimer: Metric.Timer =
    Kamon.timer(REQUEST_TIME_METRIC_NAME)
  private lazy val activeRequestsGauge: Metric.Gauge =
    Kamon.gauge(ACTIVE_REQUESTS_METRIC_NAME)
  private lazy val errorCounter = Kamon.counter(ERROR_METRIC_NAME)

  private val METHOD_TAG_NAME = "method"
  private val SERVICE_TAG_NAME = "service"
  private val ERROR_CODE_TAG_NAME = "error-code"

  def measureRequestFuture[V](serviceName: String, methodName: String)(
      requestHandling: => Future[V]
  )(implicit ec: ExecutionContext): Future[V] = {
    val maybeStartedMetrics = tryToStartMeasurement(serviceName, methodName)
    requestHandling
      .flatTap(finishMeasurement(_, maybeStartedMetrics))
      .recoverWith(handleFailedFutureMeasurement(maybeStartedMetrics))
  }

  def increaseErrorCounter(
      serviceName: String,
      methodName: String,
      errorCode: Int
  ): Counter = {
    val tags = TagSet
      .builder()
      .add(SERVICE_TAG_NAME, serviceName)
      .add(METHOD_TAG_NAME, methodName)
      .add(ERROR_CODE_TAG_NAME, errorCode.toString)
      .build()
    errorCounter.withTags(tags).increment()
  }

  private def handleFailedFutureMeasurement[V](
      maybeMeasureItems: Try[MeasureItems]
  )(implicit ec: ExecutionContext): PartialFunction[Throwable, Future[V]] = { case NonFatal(e) =>
    finishMeasurement((), maybeMeasureItems) *> Future.failed(e)
  }

  private def tryToStartMeasurement(
      serviceName: String,
      methodName: String
  ): Try[MeasureItems] =
    Try {
      val tags = TagSet
        .builder()
        .add(SERVICE_TAG_NAME, serviceName)
        .add(METHOD_TAG_NAME, methodName)
        .build()
      val taggedStartedTimer = requestTimer.withTags(tags).start()
      val incrementedActiveRequestsGauge =
        activeRequestsGauge.withTags(tags).increment()
      MeasureItems(taggedStartedTimer, incrementedActiveRequestsGauge)
    }.recoverWith { case error =>
      logger.error("Metrics start just blew up", error)
      Failure(error)
    }

  private def tryToStopMeasurement(measureItems: MeasureItems): Try[Unit] =
    Try {
      measureItems.timer.stop()
      measureItems.activeRequestsGauge.decrement()
      ()
    }.recoverWith { case error =>
      logger.error("Metrics stop just blew up", error)
      Failure(error)
    }

  private def finishMeasurement[A](
      in: A,
      maybeMeasureItems: Try[MeasureItems]
  ): Future[A] = {
    maybeMeasureItems.flatMap(tryToStopMeasurement)
    Future.successful(in)
  }

  private case class MeasureItems(
      timer: Timer.Started,
      activeRequestsGauge: Gauge
  )

  implicit class FutureMetricsOps[T](val value: Future[T]) extends AnyVal {
    def countErrorOnFail(
        serviceName: String,
        methodName: String,
        errorCode: Int
    )(implicit
        ec: ExecutionContext
    ): Future[T] =
      value.recoverWith { case NonFatal(_) =>
        increaseErrorCounter(serviceName, methodName, errorCode)
        value
      }
  }

}
