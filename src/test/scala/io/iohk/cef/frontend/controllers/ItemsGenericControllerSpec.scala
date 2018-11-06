package io.iohk.cef.frontend.controllers

import akka.http.scaladsl.model._
import akka.http.scaladsl.testkit.ScalatestRouteTest
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport
import io.iohk.cef.codecs.nio.auto._
import io.iohk.cef.data._
import io.iohk.cef.error.ApplicationError
import io.iohk.cef.frontend.controllers.common.Codecs
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar.mock
import org.scalatest.{MustMatchers, WordSpec}
import play.api.libs.json.{Format, JsValue, Json}

class ItemsGenericControllerSpec
    extends WordSpec
    with MustMatchers
    with ScalaFutures
    with ScalatestRouteTest
    with PlayJsonSupport {

  import Codecs._
  import ItemsGenericControllerSpec._

  implicit val executionContext = system.dispatcher
  val service = mock[DataItemService[BirthCertificate]]

  when(service.insert(any())).thenReturn(Right(()))

  val controller = new ItemsGenericController

  "POST /certificates" should {

    implicit val canValidate = new CanValidate[DataItem[BirthCertificate]] {
      override def validate(t: DataItem[BirthCertificate]): Either[ApplicationError, Unit] = Right(Unit)
    }
    lazy val routes =
      controller.routes[BirthCertificate]("birth-certificates", service)

    "create an item" in {
      val body =
        """
          |{
          | "content": {
          |   "id":"birth-cert",
          |   "data":
          |     {
          |       "date": "01/01/2015",
          |       "name": "Input Output HK"
          |     },
          |   "witnesses":[],
          |   "owners":[]
          |  },
          |  "tableId": "nothing",
          |  "destinationDescriptor": {
          |    "type": "everyone",
          |    "obj": {}
          |  }
          |}
        """.stripMargin
      val request = Post("/birth-certificates", HttpEntity(ContentTypes.`application/json`, body))

      request ~> routes ~> check {
        status must ===(StatusCodes.Created)

        val json = responseAs[JsValue]
        println(json)
      }
    }
  }
}

object ItemsGenericControllerSpec {

  case class BirthCertificate(date: String, name: String)

  implicit val birthCertificateFormat: Format[BirthCertificate] = Json.format[BirthCertificate]

}
