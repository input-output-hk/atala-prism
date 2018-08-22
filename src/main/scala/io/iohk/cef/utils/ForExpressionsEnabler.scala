package io.iohk.cef.utils

import scala.concurrent.{ExecutionContext, Future}
import scala.language.higherKinds
import scala.util.Try

trait ForExpressionsEnabler[F[_]] {
  def enableForExp[A](f: F[A]): ForExpressionsCapable[F, A]
  def enableForExp[A](f: => A)(implicit dummyImplicit: DummyImplicit): ForExpressionsCapable[F, A]
}

trait ForExpressionsCapable[F[_], A] {

  def map[B](f: A => B): F[B]

  def flatMap[B](f: A => F[B]): F[B]

  def pure: F[A]
}

object ForExpressionsEnabler {
  def futureEnabler(implicit executionContext: ExecutionContext): ForExpressionsEnabler[Future] = new ForExpressionsEnabler[Future] {
    override def enableForExp[A](future: Future[A]): ForExpressionsCapable[Future, A] =
      new ForExpressionsCapable[Future, A] {

        override def flatMap[B](f: A => Future[B]): Future[B] = future.flatMap(f)

        override def map[B](f: A => B): Future[B] = future.map(f)

        override def pure: Future[A] = future
      }

    override def enableForExp[A](f: => A)(implicit dummyImplicit: DummyImplicit): ForExpressionsCapable[Future, A] = enableForExp(Future(f))
  }

  val tryEnabler: ForExpressionsEnabler[Try] = new ForExpressionsEnabler[Try] {
    override def enableForExp[A](tryObj: Try[A]): ForExpressionsCapable[Try, A] = new ForExpressionsCapable[Try, A] {

      override def flatMap[B](f: A => Try[B]): Try[B] = tryObj.flatMap(f)

      override def map[B](f: A => B): Try[B] = tryObj.map(f)

      override def pure: Try[A] = tryObj
    }

    override def enableForExp[A](f: => A)(implicit dummyImplicit: DummyImplicit): ForExpressionsCapable[Try, A] = enableForExp(Try(f))
  }
}

