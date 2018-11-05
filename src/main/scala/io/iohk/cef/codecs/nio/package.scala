package io.iohk.cef.codecs

import scala.reflect.ClassTag
import scala.reflect.runtime.universe._
import io.iohk.cef.codecs.nio.components.NioCodecs

package object nio extends NioCodecs {
  private[codecs] def typeToClassTag[T](implicit tt: TypeTag[T]): ClassTag[T] = {
    val tpe = tt.tpe
    val rc = tt.mirror.runtimeClass(tpe)
    ClassTag[T](rc)
  }

}
