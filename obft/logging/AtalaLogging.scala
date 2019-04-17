package obft.logging

import org.slf4j.LoggerFactory

/**
  * Structured log facade for slf4j.
  */
trait AtalaLogging {

  protected val log = LoggerFactory.getLogger(this.getClass)

  /**
    * The idea is to have a familiar interface.
    */
  def info(message: String, ctx: (String, Loggable[_])*): Unit = {
    val data = toStructuredLog(message, ctx.toList)
    log.info(data)
  }

  /**
    * Produces a JSON with the message details
    */
  private def toStructuredLog(message: String, ctx: List[(String, Loggable[_])]): String = {
    val data = ("_message", new StringLoggable(message)) :: ctx
    data
      .map {
        case (key, value) =>
          s""" "$key": "${value.log}" """
      }
      .mkString("{", ",", "}")
  }
}

object AtalaLogging {

  trait Loggable[T] extends Any {
    def log: String
  }

  implicit class StringLoggable(val string: String) extends AnyVal with Loggable[String] {
    override def log: String = string
  }

  implicit class IntLoggable(val int: Int) extends AnyVal with Loggable[Int] {
    override def log: String = int.toString
  }
}
