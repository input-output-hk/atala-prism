package io.iohk.atala.prism.utils

// Scala 2.13 has this construct as part of the std library.
object Using {
  def using[A <: AutoCloseable, B](param: A)(f: A => B): B =
    try {
      f(param)
    } finally {
      param.close()
    }
}
