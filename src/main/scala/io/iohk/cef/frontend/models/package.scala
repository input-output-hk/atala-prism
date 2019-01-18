package io.iohk.cef.frontend

import io.iohk.cef.codecs.string._
import scala.reflect._

package object models extends PlayJson.Formats

object PlayJson {

  import play.api.libs.json.{Format, JsValue, JsResult, JsSuccess, JsError}

  trait Formats {
    implicit def PlayJsonFormatForEncodingFormat[T](
        implicit sf: Format[String],
        es: Show[T],
        ep: Parse[T],
        ct: ClassTag[T]
    ): Format[T] = new Format[T] {
      def reads(json: JsValue): JsResult[T] =
        sf.reads(json) flatMap { s =>
          ep.decode(s) match {
            case Some(t) =>
              JsSuccess(t)
            case None =>
              val name = ct.runtimeClass.getSimpleName
              JsError(s"Error trying to convert '$json' into a '$name'")
          }
        }
      def writes(t: T): JsValue =
        sf.writes(es.encode(t))
    }
  }

}
