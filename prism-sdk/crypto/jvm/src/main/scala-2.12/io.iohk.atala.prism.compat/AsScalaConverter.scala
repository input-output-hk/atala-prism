package io.iohk.atala.prism.compat

object AsScalaConverter {
  def asScalaOption[T](optional: java.util.Optional[T]): Option[T] =
    if (optional.isPresent) Some(optional.get()) else None
}
