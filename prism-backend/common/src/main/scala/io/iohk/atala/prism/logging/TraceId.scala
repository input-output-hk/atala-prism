package io.iohk.atala.prism.logging

import cats.data.ReaderT
import cats.effect.{ContextShift, IO}
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

  def liftToIOWithTraceId: IO ~> IOWithTraceIdContext = λ[IO ~> IOWithTraceIdContext](i => ReaderT.liftF(i))

  def generateYOLO: TraceId = TraceId(UUID.randomUUID().toString)

  implicit lazy val myctxLoggableCtxIO: LoggableContext[IOWithTraceIdContext] =
    LoggableContext.of[IOWithTraceIdContext].instance

  implicit def liftFuture(implicit cs: ContextShift[IO]): Lift[Future, IOWithTraceIdContext] =
    new Lift[Future, IOWithTraceIdContext] {
      override def lift[A](fa: Future[A]): IOWithTraceIdContext[A] = ReaderT.liftF(IO.fromFuture(IO(fa)))
    }
}
