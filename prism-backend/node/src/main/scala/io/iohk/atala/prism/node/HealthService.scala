package io.iohk.atala.prism.node

import _root_.grpc.health.v1.health._
import io.grpc.stub.StreamObserver
import io.iohk.atala.prism.logging.TraceId
import cats.effect.unsafe.implicits.global

import scala.concurrent.Future

/** Simple Health service, for now, the service is healthy if the request is received.
  */
class HealthService extends HealthGrpc.Health {

  private val loggerIO = TraceId.logs.forService[HealthService]

  def check(request: HealthCheckRequest): Future[HealthCheckResponse] = loggerIO
    .flatMap(logger =>
      TraceId.unLiftIOWithTraceId()(logger.info(s"Replying to health request, service = ${request.service}"))
    )
    .map(_ => servingResponse)
    .unsafeToFuture()

  def watch(
      request: HealthCheckRequest,
      responseObserver: StreamObserver[HealthCheckResponse]
  ): Unit = {
    responseObserver.onNext(
      loggerIO
        .flatMap(logger =>
          TraceId.unLiftIOWithTraceId()(logger.info(s"Watch for health changes, service = ${request.service}"))
        )
        .map(_ => servingResponse)
        .unsafeRunSync()
    )
  }

  private val servingResponse =
    HealthCheckResponse().withStatus(HealthCheckResponse.ServingStatus.SERVING)
}
