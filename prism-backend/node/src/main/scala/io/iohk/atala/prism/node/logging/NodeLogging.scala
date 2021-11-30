package io.iohk.atala.prism.node.logging

import io.iohk.atala.prism.logging.TraceId
import scalapb.GeneratedMessage
import io.iohk.atala.prism.tracing.Tracing._

import scala.concurrent.Future
import scala.util.control.NonFatal

import derevo.derive
import tofu.logging.derivation.loggable
import tofu.logging.Logs
import cats.data.ReaderT
import cats.effect.IO
import tofu.logging.LoggableContext
import tofu.logging.Logging
import cats.data.Kleisli
import scala.concurrent.ExecutionContext

@derive(loggable)
final case class TraceIdAndMethodName(traceId: String, methodName: String)

object NodeLogging {
  type IOWithTraceIdAndMethodNameContext[T] = ReaderT[IO, TraceIdAndMethodName, T]

  def logs: Logs[IO, IOWithTraceIdAndMethodNameContext] = {
    implicit lazy val loggableCtxIO = LoggableContext.of[IOWithTraceIdAndMethodNameContext].instance
    Logs.withContext[IO, IOWithTraceIdAndMethodNameContext]
  }

  def withLogIO[Response <: GeneratedMessage, Req <: GeneratedMessage](
      methodName: String,
      request: Req
  )(code: TraceId => Future[Response])(implicit
      ec: ExecutionContext,
      runtime: cats.effect.unsafe.IORuntime,
      logger: Logging[NodeLogging.IOWithTraceIdAndMethodNameContext]
  ): Future[Response] = {
    trace { traceId =>
      logger
        .info(s"methodName:$methodName; request = ${request.toProtoString}")
        .flatMap(_ =>
          Kleisli.apply { ctx: TraceIdAndMethodName =>
            IO.fromFuture(IO(code(traceId)))
              .handleErrorWith {
                case NonFatal(ex) =>
                  logger.errorCause(s"methodName:$methodName; Non Fatal Error Exception", ex).run(ctx) *>
                    IO.raiseError(ex)
                case ex => IO.raiseError(ex)
              }
          }
        )
        .flatMap(response =>
          logger.info(s"methodName:$methodName; response = ${response.toProtoString}").map(_ => response)
        )
        .run(TraceIdAndMethodName(traceId = traceId.traceId, methodName = methodName))
        .unsafeToFuture()
        .recover { case NonFatal(ex) => throw ex }
    }
  }

  def logWithTraceIdIO(
      methodName: String,
      traceId: TraceId,
      argsToLog: (String, String)*
  )(implicit logger: Logging[NodeLogging.IOWithTraceIdAndMethodNameContext]): IO[Unit] = {
    logger
      .info(s"methodName:$methodName ${argsToLog.map(x => s"${x._1}=${x._2}").mkString(",")}")
      .run(TraceIdAndMethodName(traceId = traceId.traceId, methodName = methodName))
  }
}
