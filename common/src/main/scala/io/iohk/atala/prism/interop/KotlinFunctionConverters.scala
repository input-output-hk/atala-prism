package io.iohk.atala.prism.interop

object KotlinFunctionConverters {
  class RichFunction1AsKotlinFunction[A, B](
      private val underlying: scala.Function1[A, B]
  ) extends AnyVal {
    @inline def asKotlin: kotlin.jvm.functions.Function1[A, B] =
      (p1: A) => underlying(p1)
  }

  implicit def enrichAsKotlinFunction[A, B](
      sf: scala.Function1[A, B]
  ): RichFunction1AsKotlinFunction[A, B] =
    new RichFunction1AsKotlinFunction(sf)
}
