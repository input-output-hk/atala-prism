package io.iohk.atala.prism.utils

import scala.concurrent.Future
import scala.util.Try

object syntax {
  implicit class SyntaxOps[A](exp: => A) {

    /** returns a Future containing the value without creating a new thread
      */
    def tryF: Future[A] = Future.fromTry(Try { exp })
  }
}
