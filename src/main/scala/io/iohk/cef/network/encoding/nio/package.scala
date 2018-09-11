package io.iohk.cef.network.encoding

import scala.reflect.ClassTag
import scala.reflect.runtime.universe._

package object nio extends NioCodecs {
  private[encoding] def typeToClassTag[T](implicit tt: WeakTypeTag[T]): ClassTag[T] =
    ClassTag[T](tt.mirror.runtimeClass(tt.tpe))
}
