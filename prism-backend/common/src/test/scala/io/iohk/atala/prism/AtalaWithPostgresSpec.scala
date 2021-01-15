package io.iohk.atala.prism

import scala.concurrent.ExecutionContext

import cats.effect.{IO, ContextShift, Timer}
import doobie.util.transactor.Transactor
import org.scalatest.concurrent.ScalaFutures
import io.iohk.atala.prism.repositories.PostgresRepositorySpec
import io.iohk.atala.prism.AtalaSpecBase.implicits._

class AtalaWithPostgresSpec extends PostgresRepositorySpec[IO] with ScalaFutures {
  implicit val executionContext: ExecutionContext = ExecutionContext.global
  implicit val db: Transactor[IO] = database
}

object AtalaSpecBase {
  object implicits {
    implicit val contextShift: ContextShift[IO] = IO.contextShift(ExecutionContext.global)
    implicit val timer: Timer[IO] = IO.timer(ExecutionContext.global)
  }
}
