package io.iohk.cef.frontend.models

import enumeratum._
import spray.json.{JsValue, JsonFormat, deserializationError}
import scala.reflect.ClassTag

trait SprayJsonEnum[T <: EnumEntry] { self: Enum[T] =>

  implicit def enumJsonFormat[U <: T](implicit sf: JsonFormat[String], tt: ClassTag[U]): JsonFormat[U] =
    new JsonFormat[U] {
      override def read(json: JsValue): U = {
        val txt = sf.read(json)
        self.withNameOption(txt) match {
          case Some(u: U) =>
            u
          case _ =>
            // This is horrible, it throws an exception. Maybe use some other json library?
            deserializationError(s"Expected one of '${values.map(_.entryName).mkString(", ")}', but got '$txt' instead")
        }
      }

      override def write(u: U): JsValue =
        sf.write(u.entryName)
    }

}
