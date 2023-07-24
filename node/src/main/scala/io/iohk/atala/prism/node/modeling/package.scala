package io.iohk.atala.prism.node

import shapeless.tag.@@
import shapeless.tag

package modeling {
  trait TypeCompanion[A, T] {
    def apply(a: A): A @@ T = tag[T][A](a)
    def unapply(wrapper: A @@ T): Option[A] = Some(wrapper)
  }

  abstract class ValidatedTypeCompanion[A, T](isValid: A => Boolean) {
    def apply(a: A): Option[A @@ T] =
      Some(tag[T][A](a)).filter(isValid)
    def unapply(wrapper: A @@ T): Option[A] = Some(wrapper)
  }
}
