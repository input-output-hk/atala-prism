package io.iohk.cef.utils
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.Try
import scala.language.higherKinds

trait HigherKindEnabler[F[_]] {

  def wrap[A](a: => A): F[A]

  def fromFuture[A](future: Future[A]): F[A]
}

trait HigherKindCapable[F[_], A]

object HigherKindEnabler {
  def futureEnabler(implicit executionContext: ExecutionContext): HigherKindEnabler[Future] = new HigherKindEnabler[Future] {
    override def wrap[A](a: => A): Future[A] = Future(a)
    override def fromFuture[A](future: Future[A]): Future[A] = future
  }

  def tryEnabler(timeout: FiniteDuration): HigherKindEnabler[Try] = new HigherKindEnabler[Try] {
    override def wrap[A](a: => A): Try[A] = Try(a)
    override def fromFuture[A](future: Future[A]): Try[A] = Try(Await.result(future, timeout))
  }
}
