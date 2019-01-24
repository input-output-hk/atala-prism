package io.iohk.cef.frontend.models

import enumeratum._
import play.api.libs.json._

import scala.reflect.ClassTag

trait PlayJsonEnum[T <: EnumEntry] { self: Enum[T] =>

  implicit def enumJsonFormat[U <: T](implicit sf: OFormat[String], tt: ClassTag[U]): OFormat[U] =
    new OFormat[U] {
      override def reads(json: JsValue): JsResult[U] =
        for {
          u <- sf.reads(json)
          result <- self.withNameOption(u) match {
            case Some(u: U) =>
              JsSuccess(u)
            case _ =>
              JsError(s"Expected one of '${values.map(_.entryName).mkString(", ")}', but got '$u' instead")
          }
        } yield result

      override def writes(u: U): JsObject =
        sf.writes(u.entryName)
    }

}
