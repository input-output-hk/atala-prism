package io.iohk.atala.prism.logging

import cats.data.ReaderT
import cats.effect.IO
import cats.~>
import derevo.derive
import tofu.lift.Lift
import tofu.logging.LoggableContext
import tofu.logging.derivation.loggable

import java.util.UUID
import scala.concurrent.Future

@derive(loggable)
final case class TraceId(traceId: String) extends AnyVal

object TraceId {
  type IOWithTraceIdContext[T] = ReaderT[IO, TraceId, T]

  def liftToIOWithTraceId: IO ~> IOWithTraceIdContext =
    λ[IO ~> IOWithTraceIdContext](i => ReaderT.liftF(i))

  def unLiftIOWithTraceId(
      traceId: TraceId = generateYOLO
  ): IOWithTraceIdContext ~> IO =
    λ[IOWithTraceIdContext ~> IO](_.run(traceId))

  def generateYOLO: TraceId = TraceId(UUID.randomUUID().toString)

  implicit val liftToFutureInstance: Lift[IOWithTraceIdContext, Future] =
    new Lift[IOWithTraceIdContext, Future] {
      override def lift[A](fa: IOWithTraceIdContext[A]): Future[A] = fa.run(generateYOLO).unsafeToFuture()
    }

  implicit lazy val myctxLoggableCtxIO: LoggableContext[IOWithTraceIdContext] =
    LoggableContext.of[IOWithTraceIdContext].instance

}
