package io.iohk.atala.prism.compat

import scala.jdk.CollectionConverters

object AsJavaConverter {
  def asJavaIterable[T](iterable: Iterable[T]): java.lang.Iterable[T] =
    CollectionConverters.IterableHasAsJava(iterable).asJava
}
