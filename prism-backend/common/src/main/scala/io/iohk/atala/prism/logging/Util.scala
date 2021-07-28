package io.iohk.atala.prism.logging

import cats.effect.MonadThrow
import cats.syntax.apply._
import cats.syntax.flatMap._
import cats.syntax.applicativeError._
import tofu.logging.{Loggable, Logging}
import tofu.optics.Contains
import tofu.syntax.logging._

object Util {

  def logInfoAroundResultList[F[_]: MonadThrow: Logging, A: Loggable, R](
      operationDescription: String,
      arg: A,
      tId: TraceId,
      result: F[List[R]]
  ): F[List[R]] =
    info"$operationDescription $arg $tId" *> result
      .flatTap(list => info"$operationDescription success, got ${list.size} entities $arg $tId")
      .onError { e => error"encountered an error while $operationDescription ${e.getMessage} $tId" }

  def logInfoResultById[F[_]: MonadThrow: Logging, Result, ResultId: Loggable, A: Loggable](
      operationDescription: String,
      arg: A,
      tId: TraceId,
      result: F[Result]
  )(implicit lens: Contains[Result, ResultId]): F[Result] =
    info"$operationDescription $arg $tId" *> result
      .flatTap(res => info"$operationDescription success, ${lens.extract(res)} $arg $tId")
      .onError { e =>
        error"encountered an error while $operationDescription ${e.getMessage} $tId"
      }

  def logInfoAroundResultUnit[F[_]: MonadThrow: Logging, A: Loggable](
      operationDescription: String,
      arg: A,
      tId: TraceId,
      result: F[Unit]
  ): F[Unit] =
    info"$operationDescription $arg $tId" *> result
      .flatTap(_ => info"$operationDescription success $arg $tId")
      .onError { e =>
        error"encountered an error while $operationDescription ${e.getMessage} $tId"
      }

  implicit class LogListOps[F[_], R](private val in: F[List[R]]) extends AnyVal {
    def logInfoAroundList[A: Loggable](operationDescription: String, argumentToLog: A, tId: TraceId)(implicit
        m: MonadThrow[F],
        l: Logging[F]
    ): F[List[R]] =
      logInfoAroundResultList(operationDescription, argumentToLog, tId, in)
  }

  implicit class LogResultOps[F[_], R](private val in: F[R]) extends AnyVal {
    def logInfoAround[A: Loggable, RID: Loggable](operationDescription: String, argumentToLog: A, tId: TraceId)(implicit
        m: MonadThrow[F],
        l: Logging[F],
        lens: Contains[R, RID]
    ): F[R] =
      logInfoResultById[F, R, RID, A](operationDescription, argumentToLog, tId, in)
  }

  implicit class LogUnitResultOps[F[_]](private val in: F[Unit]) extends AnyVal {
    def logInfoAroundUnit[A: Loggable](operationDescription: String, argumentToLog: A, tId: TraceId)(implicit
        m: MonadThrow[F],
        l: Logging[F]
    ): F[Unit] =
      logInfoAroundResultUnit(operationDescription, argumentToLog, tId, in)
  }

}
