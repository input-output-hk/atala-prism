package io.iohk.atala.cvp.webextension.util

import typings.inputOutputHkPrismSdk.mod.Nullable

import scala.reflect.ClassTag

object NullableOps {
  implicit class NullableOps[T: ClassTag](val x: Nullable[T]) {
    def getNullable(orElse: => T): T =
      (x.getOrElse(orElse): Any) match {
        case t: T => t
        case _ => orElse
      }
  }
}
