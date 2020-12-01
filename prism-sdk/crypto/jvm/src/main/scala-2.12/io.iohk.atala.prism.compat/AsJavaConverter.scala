package io.iohk.atala.prism.compat

import com.github.ghik.silencer.silent

import scala.jdk.CollectionConverters

object AsJavaConverter {
  def asJavaIterable[T](iterable: Iterable[T]): java.lang.Iterable[T] =
    CollectionConverters.asJavaIterable(iterable)

  @silent("type was inferred to be `Any`")
  def asJavaOptional[T](option: Option[T]): java.util.Optional[T] =
    java.util.Optional.ofNullable(option.getOrElse(null).asInstanceOf[T])
}
