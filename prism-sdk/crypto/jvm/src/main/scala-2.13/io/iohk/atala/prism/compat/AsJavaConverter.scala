package io.iohk.atala.prism.compat

import scala.jdk.CollectionConverters
import scala.jdk.OptionConverters.RichOption

object AsJavaConverter {
  def asJavaIterable[T](iterable: Iterable[T]): java.lang.Iterable[T] =
    CollectionConverters.IterableHasAsJava(iterable).asJava

  def asJavaOptional[T](option: Option[T]): java.util.Optional[T] =
    option.toJava
}
