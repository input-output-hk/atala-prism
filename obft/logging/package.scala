package obft

/**
  * The package provides utilities for structured logging.
  */
package object logging {

  /**
    * define the values that can be logged (type classes recommended).
    */
  trait Loggable[T] extends Any {
    def log: String
  }

  /**
    * Whenever you have a type that is not supposed to be logged, define an implicit value
    * created from this method to avoid accidental leaks to the logs (looking at you GitHub, Facebook, etc).
    */
  def secretLoggable[T]: Loggable[T] = new Loggable[T] {
    override def log: String = "[SECRET]"
  }

  implicit class StringLoggable(val string: String) extends AnyVal with Loggable[String] {
    override def log: String = string
  }

  implicit class IntLoggable(val int: Int) extends AnyVal with Loggable[Int] {
    override def log: String = int.toString
  }
}
