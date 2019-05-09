package atala.helpers

import monix.reactive._
import monix.eval._

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
  }
}
