package io.iohk.atala.prism.compat

import scala.jdk.OptionConverters.RichOptional

object AsScalaConverter {
  def asScalaOption[T](optional: java.util.Optional[T]): Option[T] =
    optional.toScala
}
