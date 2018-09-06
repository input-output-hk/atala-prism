package io.iohk.cef.frontend

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.{HttpEntity, StatusCode}
import io.iohk.cef.frontend.client.IdentityClientActor.{TransactionRequest}
import spray.json._

import scala.reflect.ClassTag

/**
  * Holds potential error response with the HTTP status and optional body
  *
  * @param responseStatus the status code
  * @param response the optional body
  */
case class ErrorResponseException(responseStatus: StatusCode, response: Option[HttpEntity]) extends Exception

trait DefaultJsonFormats extends DefaultJsonProtocol with SprayJsonSupport {

  /**
    * Computes ``RootJsonFormat`` for type ``A`` if ``A`` is object
    */
  def jsonObjectFormat[A: ClassTag]: RootJsonFormat[A] = new RootJsonFormat[A] {
    val ct = implicitly[ClassTag[A]]

    def write(obj: A): JsValue = JsObject("value" -> JsString(ct.runtimeClass.getSimpleName))

    def read(json: JsValue): A = ct.runtimeClass.newInstance().asInstanceOf[A]
  }

  implicit val clientRequestJsonFormat = jsonFormat5(TransactionRequest)

}
