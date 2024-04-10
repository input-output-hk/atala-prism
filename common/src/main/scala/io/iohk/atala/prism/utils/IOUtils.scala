package io.iohk.atala.prism.utils

import cats.Comonad
import cats.effect.IO
import cats.effect.unsafe.implicits.global

object IOUtils {

  implicit val ioComonad: Comonad[IO] = new Comonad[IO] {
    override def extract[A](x: IO[A]): A = x.unsafeRunSync()

    override def coflatMap[A, B](fa: IO[A])(f: IO[A] => B): IO[B] =
      IO.delay(f(fa))

    override def map[A, B](fa: IO[A])(f: A => B): IO[B] = fa.map(f)
  }

}
