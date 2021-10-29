package io.iohk.atala.prism.node.services

import cats.effect.{ContextShift, IO}

import scala.concurrent.ExecutionContext

trait CatsEffectBase {
  implicit val cs: ContextShift[IO] = IO.contextShift(ExecutionContext.global)
}
