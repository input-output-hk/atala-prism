package io.iohk.atala.prism.errors

case class LoggingContext(context: Map[String, String]) extends AnyVal {
  override def toString: String = {
    context.toList.sorted.map { case (k, v) => s"$k: $v" }.mkString(", ")
  }
}

object LoggingContext {
  def apply(pairs: (String, Any)*): LoggingContext = {
    LoggingContext(pairs.map { case (k, v) => (k, v.toString) }.toMap)
  }
}
