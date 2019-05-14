package atala

/**
  * The package provides utilities for structured logging.
  */
package object logging {

  /**
    * define the values that can be logged (type classes recommended).
    */
  trait Loggable[T] {
    def log(t: T): String
  }

  object Loggable {

    def apply[A](implicit l: Loggable[A]): Loggable[A] = l

    def gen[T](f: T => String): Loggable[T] = new Loggable[T] {
      def log(t: T): String = f(t)
    }

    /**
      * Whenever you have a type that is not supposed to be logged, define an implicit value
      * created from this method to avoid accidental leaks to the logs (looking at you GitHub, Facebook, etc).
      */
    def secret[T]: Loggable[T] = new Loggable[T] {
      override def log(t: T): String = "[SECRET]"
    }
  }

  implicit val StringLoggable: Loggable[String] = Loggable.gen[String](identity)
  implicit val IntLoggable: Loggable[Int] = Loggable.gen[Int](_.toString)
  implicit def tuple2Loggable[A: Loggable, B: Loggable]: Loggable[(A, B)] =
    Loggable.gen[(A, B)] {
      case (a, b) => s"(${Loggable[A].log(a)}, ${Loggable[B].log(b)})"
    }
}
