package io.iohk.atala.prism.utils

import cats.Comonad
import cats.effect.IO
import cats.effect.unsafe.implicits.global

object IOUtils {

  // Needed for better integration with tests, see io.iohk.atala.prism.management.console.ManagementConsoleRpcSpecBase at 57 line
  implicit val ioComonad: Comonad[IO] = new Comonad[IO] {
    override def extract[A](x: IO[A]): A = x.unsafeRunSync()

    override def coflatMap[A, B](fa: IO[A])(f: IO[A] => B): IO[B] =
      IO.delay(f(fa))

    override def map[A, B](fa: IO[A])(f: A => B): IO[B] = fa.map(f)
  }

}
