package io.iohk.atala.prism

import cats.effect.{ContextShift, IO, Timer}
import doobie.util.transactor.Transactor
import io.iohk.atala.prism.AtalaSpecBase.implicits._
import io.iohk.atala.prism.logging.TraceId
import io.iohk.atala.prism.logging.TraceId.IOWithTraceIdContext
import io.iohk.atala.prism.repositories.PostgresRepositorySpec
import org.scalatest.concurrent.ScalaFutures

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

class AtalaWithPostgresSpec extends PostgresRepositorySpec[IO] with ScalaFutures {
  implicit val executionContext: ExecutionContext = ExecutionContext.global
  implicit val db: Transactor[IO] = database
  implicit val pc: PatienceConfig = PatienceConfig(20.seconds, 5.millis)
  val dbLiftedToTraceIdIO: Transactor[IOWithTraceIdContext] =
    db.mapK(TraceId.liftToIOWithTraceId)
}

object AtalaSpecBase {
  object implicits {
    implicit val contextShift: ContextShift[IO] =
      IO.contextShift(ExecutionContext.global)
    implicit val timer: Timer[IO] = IO.timer(ExecutionContext.global)
  }
}
