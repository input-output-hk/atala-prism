package io.iohk.cef.frontend.controllers

import akka.http.scaladsl.model._
import akka.http.scaladsl.testkit.ScalatestRouteTest
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport
import io.iohk.cef.data.{DataItem, DataItemError, Owner, Witness}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{MustMatchers, WordSpec}
import play.api.libs.json.{Format, JsValue, Json}

class ItemsGenericControllerSpec
    extends WordSpec
    with MustMatchers
    with ScalaFutures
    with ScalatestRouteTest
    with PlayJsonSupport {

  import ItemsGenericControllerSpec._

  implicit val executionContext = system.dispatcher

  val controller = new ItemsGenericController()

  "POST /certificates" should {

    lazy val routes = controller.routes[BirthCertificate]("birth-certificates")

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

  case class BirthCertificate(date: String, name: String) extends DataItem[(String, String)] {
    override def id: String = "birth-certificate"

    override def data: (String, String) = (date, name)

    override def witnesses: Seq[Witness] = ???

    override def owners: Seq[Owner] = ???

    override def apply(): Either[DataItemError, Unit] = ???
  }

  implicit val format: Format[BirthCertificate] = Json.format[BirthCertificate]
}
