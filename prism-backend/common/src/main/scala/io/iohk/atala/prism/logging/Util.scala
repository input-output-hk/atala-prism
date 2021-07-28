package io.iohk.atala.prism.logging

import cats.effect.MonadThrow
import cats.syntax.apply._
import cats.syntax.flatMap._
import cats.syntax.applicativeError._
import tofu.logging.{Loggable, ServiceLogging}
import tofu.optics.Contains
import tofu.syntax.logging._

object Util {

  def logInfoAroundResultList[F[_]: MonadThrow, A: Loggable, R, S](
      operationDescription: String,
      arg: A,
      tId: TraceId,
      result: F[List[R]]
  )(implicit l: ServiceLogging[F, S]): F[List[R]] =
    info"$operationDescription $arg $tId" *> result
      .flatTap(list => info"$operationDescription success, got ${list.size} entities $arg $tId")
      .onError { e => errorCause"encountered an error while $operationDescription $tId" (e) }

  def logInfoResultById[F[_]: MonadThrow, Result, ResultId: Loggable, A: Loggable, S](
      operationDescription: String,
      arg: A,
      tId: TraceId,
      result: F[Result]
  )(implicit lens: Contains[Result, ResultId], l: ServiceLogging[F, S]): F[Result] =
    info"$operationDescription $arg $tId" *> result
      .flatTap(res => info"$operationDescription success, ${lens.extract(res)} $arg $tId")
      .onError { e =>
        errorCause"encountered an error while $operationDescription $tId" (e)
      }

  def logInfoAroundResultUnit[F[_]: MonadThrow, A: Loggable, S](
      operationDescription: String,
      arg: A,
      tId: TraceId,
      result: F[Unit]
  )(implicit l: ServiceLogging[F, S]): F[Unit] =
    info"$operationDescription $arg $tId" *> result
      .flatTap(_ => info"$operationDescription success $arg $tId")
      .onError { e =>
        errorCause"encountered an error while $operationDescription $tId" (e)
      }

  implicit class LogListOps[F[_], R](private val in: F[List[R]]) extends AnyVal {
    def logInfoAroundList[A: Loggable, S](operationDescription: String, argumentToLog: A, tId: TraceId)(implicit
        m: MonadThrow[F],
        l: ServiceLogging[F, S]
    ): F[List[R]] =
      logInfoAroundResultList(operationDescription, argumentToLog, tId, in)
  }

  implicit class LogResultOps[F[_], R](private val in: F[R]) extends AnyVal {
    def logInfoAround[A: Loggable, RID: Loggable, S](operationDescription: String, argumentToLog: A, tId: TraceId)(
        implicit
        m: MonadThrow[F],
        l: ServiceLogging[F, S],
        lens: Contains[R, RID]
    ): F[R] =
      logInfoResultById[F, R, RID, A, S](operationDescription, argumentToLog, tId, in)
  }

  implicit class LogUnitResultOps[F[_]](private val in: F[Unit]) extends AnyVal {
    def logInfoAroundUnit[A: Loggable, S](operationDescription: String, argumentToLog: A, tId: TraceId)(implicit
        m: MonadThrow[F],
        l: ServiceLogging[F, S]
    ): F[Unit] =
      logInfoAroundResultUnit(operationDescription, argumentToLog, tId, in)
  }

}
