package atala.helpers

import scala.concurrent.Future
import monix.execution.Ack
import monix.execution.Ack.Stop
import monix.reactive._
import monix.eval._

import scala.util.control.NonFatal

package object monixhelpers {

  implicit class GeneralObservableOps[A](val o: Observable[A]) extends AnyVal {

    def oneach[R](f: A => R): Observable[A] =
      o.map { r =>
        f(r)
        r
      }

  }

  implicit class GeneralObserverOps[A](val o: Observer[A]) extends AnyVal {

    def feedItem(item: A)(implicit s: monix.execution.Scheduler): Unit = {
      Task.deferFuture(o.onNext(item)).runAsync
    }

    /**
      * I've copied this from here:
      * https://github.com/monix/monix/blob/master/monix-reactive/shared/src/main/scala/monix/reactive/Observer.scala
      *
      * This hasn't made it's way into any release yet (not even 3.0.0-RC2). Whenever it's available, we will be able
      * to delete this
      */
    def contramap[B](f: B => A): Observer[B] =
      new ContravariantObserver(o)(f)
  }

  /**
    * I've copied this from here:
    * https://github.com/monix/monix/blob/master/monix-reactive/shared/src/main/scala/monix/reactive/Observer.scala
    *
    * This hasn't made it's way into any release yet (not even 3.0.0-RC2). Whenever it's available, we will be able
    * to delete this
    */
  private[this] final class ContravariantObserver[A, B](source: Observer[A])(f: B => A) extends Observer[B] {
    // For protecting the contract
    private[this] var isDone = false

    override def onNext(elem: B): Future[Ack] = {
      if (isDone) Stop
      else {
        var streamError = true
        try {
          val b = f(elem)
          streamError = false
          source.onNext(b)
        } catch {
          case NonFatal(ex) if streamError =>
            onError(ex)
            Stop
        }
      }
    }
    override def onError(ex: Throwable): Unit =
      if (!isDone) {
        isDone = true; source.onError(ex)
      }
    override def onComplete(): Unit =
      if (!isDone) {
        isDone = true; source.onComplete()
      }
  }

}
