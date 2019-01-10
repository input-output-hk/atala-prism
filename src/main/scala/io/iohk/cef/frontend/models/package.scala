package io.iohk.cef.frontend

import io.iohk.cef.codecs.string._
import scala.reflect._

package object models extends Spray.Formats with PlayJson.Formats

object Spray {

  import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
  import spray.json._

  trait Formats extends DefaultJsonProtocol with SprayJsonSupport {

    implicit def SprayJsonFormatForEncodingFormat[T](
        implicit sf: JsonFormat[String],
        es: Show[T],
        ep: Parse[T],
        ct: ClassTag[T]
    ): JsonFormat[T] = new JsonFormat[T] {
      override def read(json: JsValue): T = {
        ep.decode(sf.read(json)) match {
          case Some(t) =>
            t
          case None =>
            val name = ct.runtimeClass.getSimpleName
            // This is horrible, it throws an exception. Maybe use some other json library?
            deserializationError(s"Error trying to convert '$json' into a '$name'")
        }
      }

      override def write(t: T): JsValue =
        sf.write(es.encode(t))
    }

  }

}

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
