package io.iohk.cef.frontend.controllers

import akka.http.scaladsl.model._
import akka.http.scaladsl.testkit.ScalatestRouteTest
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport
import io.iohk.cef.data.DataItem
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{MustMatchers, WordSpec}
import play.api.libs.json._
import io.iohk.cef.frontend.controllers.common.Codecs._

class ItemsGenericControllerSpec
    extends WordSpec
    with MustMatchers
    with ScalaFutures
    with ScalatestRouteTest
    with PlayJsonSupport {

  implicit val executionContext = system.dispatcher

  val controller = new ItemsGenericController()

  "POST /certificates" should {

    import ItemsGenericControllerSpec._

    implicit val diFormat = dataItemFormat[BirthCertificate](Json.format[BirthCertificate])

    lazy val routes = controller.routes[DataItem[BirthCertificate]]("birth-certificates")

    "create a valid key-pair" in {
      val body =
        """
          |{
          |  "date": "01/01/2015",
          |  "name": "Input Output HK"
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
}
