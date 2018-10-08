package io.iohk.cef.frontend

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json._
import io.iohk.cef.network.encoding.string._
import scala.reflect._

package object models extends DefaultJsonProtocol with SprayJsonSupport {

  implicit def JsonFormatForEncodingFormat[T](
      implicit sf: JsonFormat[String],
      es: Show[T],
      ep: Parse[T],
      ct: ClassTag[T]): JsonFormat[T] = new JsonFormat[T] {
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

//  import akka.util.ByteString
//  import org.bouncycastle.util.encoders.Hex
//
//  val byteStringJsonFormat: JsonFormat[ByteString] = new JsonFormat[ByteString] {
//    override def read(jsValue: JsValue): ByteString = jsValue match {
//      case JsString(value) =>
//        ByteString(Hex.decode(value))
//      case _ =>
//        deserializationError(s"Attempting to decode $jsValue as a JsString encoded binary.")
//    }
//
//    override def write(b: ByteString): JsValue =
//      JsString(Hex.toHexString(b.toArray))
//  }

}
