package io.iohk.cef.utils

import scala.concurrent.{ExecutionContext, Future}
import scala.language.higherKinds
import scala.util.Try

trait ForExpressionsEnabler[F[_]] {
  def enableForExp[A](f: F[A]): ForExpressionsCapable[F]
  def enableForExp[A](f: A)(implicit dummyImplicit: DummyImplicit): ForExpressionsCapable[F]
}

trait ForExpressionsCapable[F[_]] {
  type A

  def map[B](f: A => B): F[B]

  def flatMap[B](f: A => F[B]): F[B]
}

object ForExpressionsEnabler {
  def futureEnabler(implicit executionContext: ExecutionContext): ForExpressionsEnabler[Future] =
    new ForExpressionsEnabler[Future] {
      override def enableForExp[Value](future: Future[Value]): ForExpressionsCapable[Future] =
        new ForExpressionsCapable[Future] {
          override type A = Value

          override def flatMap[B](f: A => Future[B]): Future[B] = future.flatMap(f)

          override def map[B](f: A => B): Future[B] = future.map(f)
        }

      override def enableForExp[A](f: A)(implicit dummyImplicit: DummyImplicit): ForExpressionsCapable[Future] =
        enableForExp(Future(f))
    }

  val tryEnabler: ForExpressionsEnabler[Try] = new ForExpressionsEnabler[Try] {
    override def enableForExp[Value](tryObj: Try[Value]): ForExpressionsCapable[Try] = new ForExpressionsCapable[Try] {
      override type A = Value

      override def flatMap[B](f: A => Try[B]): Try[B] = tryObj.flatMap(f)

      override def map[B](f: A => B): Try[B] = tryObj.map(f)
    }

    override def enableForExp[A](f: A)(implicit dummyImplicit: DummyImplicit): ForExpressionsCapable[Try] =
      enableForExp(Try(f))
  }
}
