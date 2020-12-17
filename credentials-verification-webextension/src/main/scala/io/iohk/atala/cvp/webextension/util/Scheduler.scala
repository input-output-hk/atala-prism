package io.iohk.atala.cvp.webextension.util

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{Future, Promise}

/**
  * Pretty simple scheduler that aims to work in Scala/Scalajs
  */
trait Scheduler {

  /**
    * Executes the given handler after the given delay
    */
  def after[T](delay: FiniteDuration)(handler: => Future[T]): Future[T]
}

object Scheduler {
  object JsScheduler extends Scheduler {
    override def after[T](delay: FiniteDuration)(handler: => Future[T]): Future[T] = {
      val promise = Promise[T]()
      val f: scalajs.js.Function0[Unit] = () => {
        promise.completeWith(handler)
      }

      org.scalajs.dom.window.setTimeout(f, delay.toMillis.toDouble)

      promise.future
    }
  }

  object Implicits {
    implicit val jsScheduler: Scheduler = JsScheduler
  }
}
